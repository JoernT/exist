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

    private final String HMAC_KEY = "foobar";
    private final String HMAC_ALG = "HmacSHA256";
    private final String TOKEN_SEPARATOR = "|";

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
                //try to get repo.xml
                auth = initAppAuth(broker, appName);
                //if there's none just return
                if (auth == null) return user;
            }

            Subject subject = isTokenValid(request, broker, appName);
            if (subject != null) {

                //check for a logout
                String logout = auth.getLogoutEndpoint();
                if (requestPath.endsWith(logout)) {
                    String userName = subject.getName();
                    UserAuth.getInstance().removeUserAuth(userName);
                    String logoutRedirect = auth.getLogoutRedirect();
                    response.setContentType("text/html");
                    // todo: this needs generification in case an app is running on root
                    response.sendRedirect(request.getContextPath() + "/apps/" + appName + "/" + logoutRedirect);
                }

                return subject;
            }

            if (isProtected(auth, requestPath)) {
                return performLogin(request, response, broker, config);
            }


        }

        return user; //default to returning the user that was passed in (usually guest)

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

        if (auth != null) {
            List urls = auth.getWhiteList();
            for (Object url1 : urls) {
                String url = (String) url1;
                //todo: refine - just checking if requestURI ends with a whitelisted item - apply some regex (glob support)?
                if (requestPath.endsWith(url)) {
                    return false; //url is whitelisted
                }
            }
            return true;
        }
        // if there's no AppAuth
        return false;
    }

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

    private Subject isTokenValid(HttpServletRequest request, DBBroker broker, String appName) throws EXistException, AuthenticationException {
        String username;
        username = validateToken(getCookieValue(request), appName);
        if (username != null) {
            LoginDetails details = UserAuth.getInstance().fetchLoginDetails(username);
            //do login
            return login(username, details.getPass(), broker);

        }else{
            //todo: username is always null
            UserAuth.getInstance().removeUserAuth(username);
        }

        return null;
    }

    private Subject performLogin(HttpServletRequest request, HttpServletResponse response, DBBroker broker, ServletConfig config) throws ServletException, IOException {
        String appName = getAppNameFromRequest(request);
        String loginPage = RepoAuthCache.getInstance().getAuthInfo(appName).getLoginEndpoint();

        Subject subject;

        //try to get username
        String username = request.getParameter("user");
        String pass = request.getParameter("password");

        if (username == null) {
            //redirect to login endpoint
            response.setContentType("text/html");
            RequestDispatcher dispatcher = config.getServletContext().getRequestDispatcher("/apps/" + appName + "/" + loginPage);
            dispatcher.forward(request, response);
//            return null;
        } else {
            // login attempt if username is given
            try {
                // authenticate with eXistdb
                subject = login(username, pass, broker);
                UserAuth.getInstance().registerUser(username, pass, appName);
                setCookie(response, subject.getName());
                return subject;
            } catch (EXistException e) {
                throw new ServletException(e);
            } catch (AuthenticationException e) {
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
        //// TODO: create token
        // username + expiry date
//        String token = auth.createToken(username);
        String token = createToken(username);
//        String cookieName = auth.getCookieName();

        Cookie cookie1 = new Cookie(DEFAULT_COOKIE_NAME, token);
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
        Document repoXml = broker.getXMLResource(XmldbURI.xmldbUriFor("xmldb:exist:///db/apps/" + appName + "/repo.xml"));

        if (repoXml != null) {
            AppAuth auth = new AppAuth();
            Element authElem = DOMUtil.getChildElementByLocalName(repoXml, "authentication");
            if (authElem != null) {
                if(authElem.hasAttribute("lifetime")){
                    String s = authElem.getAttribute("lifetime");
                    auth.setLifeTime(Integer.parseInt(s));
                }
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
                    if(logoutEndpoint.hasAttribute("redirect")){
                        auth.setLogoutRedirect(logoutEndpoint.getAttribute("redirect"));
                    }
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


    private String createToken(String username) {
        try {
            String hmac = calcHMAC(username);
            return username + TOKEN_SEPARATOR + hmac;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String validateToken(String token, String appName) {
        if (token == null) {
            return null;
        }
        String hmac;
        String[] fields;
        try {
            fields = token.split(Pattern.quote(TOKEN_SEPARATOR));
            hmac = calcHMAC(fields[0]);
        } catch (PatternSyntaxException | UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null; //todo: check: shouldn't we return null in case the token cannot be parsed or calculated?
        }

        UserAuth userAuth = UserAuth.getInstance();
        LoginDetails details = userAuth.fetchLoginDetails(fields[0]);
        if (details == null) {
            //we might run here in case there's an old cookie containing a parseable token
            return null;
        }

        long now = Instant.now().getEpochSecond();
        long lastAccessed = details.getLastAccessed(appName);

        AppAuth auth = RepoAuthCache.getInstance().getAuthInfo(appName);

//        if (hmac.equals(fields[1]) && now < lastAccessed + tokenLifetime) {
        if (hmac.equals(fields[1]) && now < lastAccessed + auth.getLifeTime()) {
            details.updateLastAccessed(appName);
            return fields[0];
        } else {
            return null;
        }
    }

    private String calcHMAC(String username) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
//        String tokdata = username + TOKEN_SEPARATOR + tstamp;

        try {
            Mac hmac = Mac.getInstance(HMAC_ALG);
            byte[] byteKey = HMAC_KEY.getBytes("ASCII");
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_ALG);
            hmac.init(keySpec);
            byte[] hmac_data = hmac.doFinal(username.getBytes("UTF-8"));
            Formatter formatter = new Formatter();
            for (byte b : hmac_data) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException |
                InvalidKeyException e) {
            throw e;
        }

    }




}
