/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001-2018 The eXist Project
 * http://exist-db.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.exist.http.urlrewrite.appauth;

import org.exist.EXistException;
import org.exist.security.AuthenticationException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SecurityManager;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.util.DOMUtil;
import org.exist.xmldb.XmldbURI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AppAuthenticator {
    private static final String DEFAULT_COOKIE_NAME = "AppAuth";

    /*
    * Authenticate the given user according to configuration given in the repo.xml of the requested app.
    *
    * In case an <authentication> Element is found in repo.xml the requested path will be handled by this Authenticator.
    *
    * If no <authentication> Element is given in repo.xml this method just exists to allow backward-compatible
    * processing.
    *
    * todo: review - logic is not right yet
    *
    */
    public Subject authenticate(HttpServletRequest request,
                                HttpServletResponse response,
                                Subject user,
                                BrokerPool pool,
                                ServletConfig config) throws ServletException, PermissionDeniedException, URISyntaxException, IOException, EXistException, AuthenticationException {

        String appName = getAppNameFromRequest(request);
        String requestPath = request.getRequestURI();

        try (final DBBroker broker = pool.get(Optional.ofNullable(user))) {

            AppAuth auth = RepoAuthCache.getInstance().getAuthInfo(appName);
            if (auth == null) {
		Logger.log("debug", "authenticate: auth cache is null");
                // try to parse authentication element in repo.xml
		// XXX this is performance punishment for old compat authentication
                auth = initAppAuth(broker, appName);
                // if still null, fallback to old compat authentication
                if (auth == null) {
		    Logger.log("warning", "authenticate: fallback to old compat authentication");
		    return user;
		}
            }

            Subject subject = isTokenValid(request, broker, appName);
            if (subject != null) {
		Logger.log("debug", "authenticate: got valid token");

                // check for a logout
                String logout = auth.getLogoutEndpoint();
                if (requestPath.endsWith(logout)) {
		    Logger.log("debug", "authenticate: logout");
                    String userName = subject.getName();
                    UserAuth.getInstance().removeUserDetails(userName);
                    String logoutRedirect = auth.getLogoutRedirect();
                    response.setContentType("text/html");
                    // todo: this needs generification in case an app is running on root
                    response.sendRedirect(request.getContextPath() + "/apps/" + appName + "/" + logoutRedirect);
                }

		// return from authenticate() since we have a valid token
		Logger.log("debug", "authenticate: grant token authenticated access to "+requestPath);
                return subject;
            }

	    // if no valid token and URI requires auth, perform login
            if (isProtected(auth, requestPath)) {
		Logger.log("debug", "authenticate: perform login for access to "+requestPath);
                return performLogin(request, response, broker, config);
            }

	}  // end try

	// if we came here, there was no valid token and the URI does not
	// require authentication. Return the user that was passed in (usually
	// guest) for unauthenticated access.
	Logger.log("debug", "authenticate: grant unauth access to "+requestPath);
        return user;
    }

    /**
     * checks requestPath against whitelist of URLs for respective AppAuth. The check
     * will take place only for apps that use an <authentication> element in their repo.xml.
     *
     * @param auth the AppAuth object for the requested app
     * @param requestPath the requestPath
     * @return true if requestPath is NOT found in whitelist.
     */
    private boolean isProtected(AppAuth auth, String requestPath) {

        if (auth == null) {
	    Logger.log("debug", "isProtected: no AppAuth");
	    return false;
	} 

	List urls = auth.getWhiteList();
	for (Object url1 : urls) {
	    String url = (String) url1;
	    //todo: refine - just checking if requestURI ends with a whitelisted item - apply some regex (glob support)?
	    if (requestPath.endsWith(url)) {
		Logger.log("debug", "isProtected: whitelisted: "+requestPath);
		return false;
	    }
	}
	// if URI not found in whitelist, it is a protected ressource
	Logger.log("debug", "isProtected: protected: "+requestPath);
	return true;
    }

    /* fetch AppAuth cookie which contains the auth token */
    private String getCookieValue(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie1 : cookies) {
            if (cookie1.getName().equals(DEFAULT_COOKIE_NAME)) {
                return cookie1.getValue();
            }
        }
        return null;
    }

    /* Fetch auth token from HTTP cookie, login user if token valid. */
    private Subject isTokenValid(HttpServletRequest request, DBBroker broker, String appName) throws EXistException, AuthenticationException {

	// username=null if token cannot be validated
        String username = AuthTokenFactory.getInstance().validateToken(getCookieValue(request), appName);
        if (username != null) {
	    Logger.log("debug", "token is valid");
            LoginDetails details = UserAuth.getInstance().getUserDetails(username);
            // do login
            return existdbLogin(username, details.getPass(), broker);

        } else {
	    Logger.log("notice", "token is INVALID");
	    // XXX what is this for? logout?
            //todo: username is always null
            UserAuth.getInstance().removeUserDetails(username);
        }

        return null;
    }

    /* This method is called in two cases:
     * - after the user has entered username/password into a login form, fetch
     *   username/password parameters and login to eXist;
     * - if no username parameter present, user's browser gets redirected to a
     *   login form page.
     * XXX consider splitting into separate methods for clarity
     */
    private Subject performLogin(HttpServletRequest request, HttpServletResponse response, DBBroker broker, ServletConfig config) throws ServletException, IOException {

        String appName = getAppNameFromRequest(request);
        String loginPage = RepoAuthCache.getInstance().getAuthInfo(appName).getLoginEndpoint();
        Subject subject;

        // try to get username and password parameters
        String username = request.getParameter("user");
        String pass = request.getParameter("password");

        if (username == null) {
	    Logger.log("debug", "performLogin: redirect to login page");
            // redirect to login endpoint
            response.setContentType("text/html");
            RequestDispatcher dispatcher = config.getServletContext().getRequestDispatcher("/apps/" + appName + "/" + loginPage);
            dispatcher.forward(request, response);
//            return null;
        } else {
            // login attempt if username is given
            try {
		Logger.log("debug", "performLogin: trying to login");
                // authenticate with eXistdb
                subject = existdbLogin(username, pass, broker);
		Logger.log("info", "performLogin: successful login, user "+username);
                UserAuth.getInstance().registerUserDetails(username, pass, appName);
                setCookie(response, subject.getName());
                return subject;
            } catch (EXistException e) {
		Logger.log("notice", "performLogin: error logging in user "+username+": "+e.getMessage());
                throw new ServletException(e);
            } catch (AuthenticationException e) {
		Logger.log("notice", "performLogin: failed login, user "+username+": "+e.getMessage());
                //future todo: login counter?
                String loginFailed = RepoAuthCache.getInstance().getAuthInfo(appName).getLoginFailed();
                response.setContentType("text/html");
                response.sendRedirect("/apps/" + appName + "/" + loginFailed);
//                return null;
            }
        }
        return null;
    }

    private void setCookie(HttpServletResponse response, String username) {
        String token = AuthTokenFactory.getInstance().createToken(username);
        Cookie cookie1 = new Cookie(DEFAULT_COOKIE_NAME, token);
        response.addCookie(cookie1);
    }

    private Subject existdbLogin(final String user, final String pass, DBBroker broker) throws EXistException, AuthenticationException {
        final SecurityManager sm = BrokerPool.getInstance().getSecurityManager();
        final Subject subject = sm.authenticate(user, pass);
//        broker.popSubject();
        broker.pushSubject(subject);
        return subject;
    }

    /* Methods to create an AppAuth data instance from repo.xml.
     * These are run only once on app initialization.
     */

    /* Parse repo.xml and create AppAuth data from "authentication" element.
     * Throw exception if repo.xml cannot be accessed.
     * Return null if there is no "authentication" element in repo.xml.
     * Otherwise return AppAuth instance inititialized from system defaults,
     * possibly overridden by settings in the "authentication" element.
     */
    private AppAuth initAppAuth(DBBroker broker, String appName) throws PermissionDeniedException, URISyntaxException {
	// throws exceptions if repo.xml cannot be accessed
        Document repoXml = broker.getXMLResource(XmldbURI.xmldbUriFor("xmldb:exist:///db/apps/" + appName + "/repo.xml"));

        if (repoXml == null) {
	    Logger.log("warning", "initAppAuth: repoXml is null");
	    return null;
	}

	AppAuth auth;
	Element authElem = DOMUtil.getChildElementByLocalName(repoXml, "authentication");
	if (authElem == null) {
	    Logger.log("info", "initAppAuth: no authentication element in repo.xml, fallback to compat behavior");
	    return null;
	}

	auth = new AppAuth();
	if (authElem.hasAttribute("lifetime")) {
	    String s = authElem.getAttribute("lifetime");
	    auth.setLifeTime(Integer.parseInt(s));
	}
	auth.setWhiteList(parseWhitelistUrls(authElem));

	Element mechanism = DOMUtil.getChildElementByLocalName(authElem, "mechanism");
	if (mechanism != null) {
	    Element loginEndPoint = DOMUtil.getChildElementByLocalName(mechanism, "login-endpoint");
	    if (loginEndPoint != null) {
		auth.setLoginEndpoint(loginEndPoint.getTextContent());
		auth.setLoginFailed(loginEndPoint.getAttribute("redirect-on-fail"));
	    }
	    Element logoutEndpoint = DOMUtil.getChildElementByLocalName(mechanism, "logout-endpoint");
	    if (logoutEndpoint != null) {
		auth.setLogoutEndpoint(logoutEndpoint.getTextContent());
		if (logoutEndpoint.hasAttribute("redirect")) {
		    auth.setLogoutRedirect(logoutEndpoint.getAttribute("redirect"));
		}
	    }
	}
        
	RepoAuthCache.getInstance().setAuthInfo(appName, auth);
	AuthTokenFactory.getInstance().setLifetime(auth.getLifeTime());
	return auth;
    }

    /* Parse the "allowed" element and create a list of URI strings that are
     * whitelisted, so authentication gets bypassed for these URIs.
     */
    private List parseWhitelistUrls(Element authElem) {
        List allowed = new ArrayList();
        Element allowedElem = DOMUtil.getChildElementByLocalName(authElem, "allowed");
        if (allowedElem != null) {
            // get uri elements and iterate them
            List<Element> list = DOMUtil.getChildElements(allowedElem);
            for (Element element : list) {
                if (element.getLocalName().equals("uri")) {
                    String uriString = element.getTextContent();
                    allowed.add(uriString);
                }
            }
        }
        return allowed;
    }


    //todo: this is fragile and must be improved - how can we determine the apps name from the request in a secure way?
    private String getAppNameFromRequest(HttpServletRequest request) {
        String s = request.getRequestURI();
        String[] tokens = s.split("/");
        if (tokens.length > 2) {
            return tokens[3];
        } else {
            return null;
        }
    }

}
