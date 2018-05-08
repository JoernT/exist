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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppAuthenticator {

    /*
    * Authenticate the given user according to configuration given in the repo.xml of the requested app.
    *
    * In case an <authentication> Element is found in repo.xml the requested path will be handled by this Authenticator.
    *
    * If no <authentication> Element is given in repo.xml this method just exists to allow backward-compatible
    * processing.
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
                //try to get repo.xml
                auth = initAppAuth(broker, appName);
                //if there's none just return
                if (auth == null) return user;
            }

            Subject subject = isTokenValid(request, broker);
            if (subject != null) {
                return subject;
            }

            if (isProtected(auth, requestPath)) {
                return performLogin(request, response, broker, config);
            }

            String logout = auth.getLogoutEndpoint();
            String logoutUrl = requestPath + "?" + request.getQueryString();
            if (logoutUrl.indexOf(logout) != -1) {
                //todo: invalidate user login
                String userName = subject.getName();
                UserAuth.getInstance().removeUserAuth(userName);
                response.setContentType("text/html");
                String logoutRedirect = auth.getLogoutRedirect();
                response.sendRedirect("/apps/" + appName + "/" + logoutRedirect);
            }

        }

        return user; //default to returning the user that was passed in (usually guest)

    }


    private boolean isProtected(AppAuth auth, String requestPath) throws ServletException, URISyntaxException, PermissionDeniedException {

        if (auth != null) {
            List urls = auth.getWhiteList();

            //todo: this may be more efficient?
            for (int i = 0; i < urls.size(); i++) {
                String url = (String) urls.get(i);

                //todo: refine - just checking if requestURI contains a whitelisted item
                if (requestPath.indexOf(url) != -1) {
                    return false; //url is whitelisted
                }

            }
            return true;
        }
        // if there's no AppAuth
        return false;

    }

    private String getCookieValue(HttpServletRequest request){
        UserAuth userAuth = UserAuth.getInstance();

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String cookieName = userAuth.getCookieName();

        //todo: exit loop when cookie is found
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie1 = cookies[i];
            if (cookie1.getName().equals(cookieName)) {
                return cookie1.getValue();
            }
        }
        return null;
    }

    private Subject isTokenValid(HttpServletRequest request, DBBroker broker) throws EXistException, AuthenticationException {
        UserAuth userAuth = UserAuth.getInstance();
        String username = null;

        username = userAuth.validateToken(getCookieValue(request));
        if (username != null) {
            LoginDetails details = UserAuth.getInstance().fetchLoginDetails(username);
            //do login
            return login(username, details.getPass(), broker);

        }else{
            UserAuth.getInstance().removeUserAuth(username);
        }

        return null;
    }

    private Subject performLogin(HttpServletRequest request, HttpServletResponse response, DBBroker broker, ServletConfig config) throws ServletException, IOException {
        String appName = getAppNameFromRequest(request);
//        String loginPage = getLoginEndpoint(); // todo: will just return 'login.html' as default
        String loginPage = RepoAuthCache.getInstance().getAuthInfo(appName).getLoginEndpoint();

        Subject subject = null;

        //try to get username
        String username = request.getParameter("user");
        String pass = request.getParameter("password");

        if (username == null) {
            //redirect to login endpoint
            response.setContentType("text/html");
            RequestDispatcher dispatcher = config.getServletContext().getRequestDispatcher("/apps/" + appName + "/" + loginPage);
            dispatcher.forward(request, response);
            return null;
        } else {
            // login attempt if username is given
            try {
                // authenticate with eXistdb
                // todo: return value 'user' needed at all?
                subject = login(username, pass, broker);
                UserAuth.getInstance().registerUser(username, pass);
                setCookie(response, subject.getName(), UserAuth.getInstance());
                return subject;
            } catch (EXistException e) {
                throw new ServletException(e);
            } catch (AuthenticationException e) {
                //future todo: login counter?
                //todo: change to redirect

                String loginFailed = RepoAuthCache.getInstance().getAuthInfo(appName).getLoginFailed();
                response.setContentType("text/html");
                // as this login attempt failed we append a 'failed=true' parameter to the request. This can be picked up by a client for displaying an error
//                RequestDispatcher dispatcher = config.getServletContext().getRequestDispatcher("/apps/" + appName + "/" + loginFailedUrl);
                RequestDispatcher dispatcher = config.getServletContext().getRequestDispatcher("/apps/" + appName + "/" + loginFailed);
                dispatcher.forward(request, response);
                return null;
            }
        }

    }

    private void setCookie(HttpServletResponse response, String username, UserAuth auth) {
        //// TODO: create token
        // username + expiry date
        String token = auth.createToken(username);
        String cookieName = auth.getCookieName();

        Cookie cookie1 = new Cookie(cookieName, token);
        response.addCookie(cookie1);
    }


    private Subject login(final String user, final String pass, DBBroker broker) throws EXistException, AuthenticationException {
        final SecurityManager sm = BrokerPool.getInstance().getSecurityManager();
        final Subject subject = sm.authenticate(user, pass);
//        broker.popSubject();
        broker.pushSubject(subject);
        return subject;
    }


    private AppAuth initAppAuth(DBBroker broker, String appName) throws PermissionDeniedException, URISyntaxException {
        Document repoXml = null;
        repoXml = broker.getXMLResource(XmldbURI.xmldbUriFor("xmldb:exist:///db/apps/" + appName + "/repo.xml"));

        if (repoXml != null) {
            AppAuth auth = new AppAuth();
            // ### try to get list of whitelisted urls from cache
            Element authElem = DOMUtil.getChildElementByLocalName(repoXml, "authentication");
            if (authElem != null) {
                auth.setUrls(getWhitelistUrls(authElem));
            } else {
                return null;
            }

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
                    auth.setLogoutRedirect(loginEndPoint.getAttribute("redirect"));
                }
            }
            RepoAuthCache.getInstance().setAuthInfo(appName, auth);
            return auth;
        }
        return null;
    }

    private List getWhitelistUrls(Element authElem) {
        List allowed = new ArrayList();
        Element allowedElem = DOMUtil.getChildElementByLocalName(authElem, "allowed");
        if (allowedElem != null) {
            //get uri elements and iterate them
            List<Element> list = DOMUtil.getChildElements(allowedElem);
            for (int i = 0; i < list.size(); i++) {
                Element element = list.get(i);
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
