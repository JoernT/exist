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

package org.exist.http.urlrewrite;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AppAuth {

    private final String HMAC_KEY = "foobar";
    private final String HMAC_ALG = "HmacSHA256";
    private final String TOKEN_SEPARATOR = "|";
    private final int TOKEN_LIFETIME = 300;  // 5min
    private final String DEFAULT_COOKIE_NAME = "AppAuth";

    private String loginEndpoint;
    private String logoutEndpoint;
    private String loginFailed;
    private List urls = new ArrayList();
    private String userName = null;

    public AppAuth() {
        loginEndpoint = null;
    }

    public String getLoginEndpoint() {
        return this.loginEndpoint;
    }

    public void setLoginEndpoint(String endPoint) {
        this.loginEndpoint = endPoint;
        if (!this.urls.contains(endPoint)) {
            this.urls.add(endPoint);
        }
    }

    public String getLogoutEndpoint() {
        return this.logoutEndpoint;
    }

    public void setLogoutEndpoint(String endPoint) {
        this.logoutEndpoint = endPoint;
        if (!this.urls.contains(endPoint)) {
            this.urls.add(endPoint);
        }
    }

    public String getLoginFailed() {
        return this.loginFailed;
    }

    public void setLoginFailed(String loginFailed) {
        this.loginFailed = loginFailed;
        if (this.urls.contains(loginFailed)) {
            this.urls.add(loginFailed);
        }
    }

    public void setUrls(List urls) {
        this.urls = urls;
    }

    public List getWhiteList() {
        return this.urls;
    }

    public String createToken(String username) {
        long expires = Instant.now().getEpochSecond() + TOKEN_LIFETIME;
        try {
            String hmac = calcHMAC(username, Long.toString(expires));
            return username + TOKEN_SEPARATOR + expires + TOKEN_SEPARATOR + hmac;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getUserName() {
        return userName;
    }

    public boolean validateToken(String token) {
        String hmac = null;
        String[] fields = new String[3];
        try {
            fields = token.split(Pattern.quote(TOKEN_SEPARATOR));
            hmac = calcHMAC(fields[0], fields[1]);
        } catch (PatternSyntaxException |  UnsupportedEncodingException | NoSuchAlgorithmException| InvalidKeyException e) {
            e.printStackTrace();
        }
        long now = Instant.now().getEpochSecond();
        long l = Long.parseLong(fields[1]);
        if (hmac.equals(fields[2]) && now <= l) {
            this.userName = fields[0];
            return true;
        } else {
            return false;
        }
    }

    private String calcHMAC(String username, String tstamp) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String tokdata = username + TOKEN_SEPARATOR + tstamp;

        try {
            Mac hmac = Mac.getInstance(HMAC_ALG);
            byte[] byteKey = HMAC_KEY.getBytes("ASCII");
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_ALG);
            hmac.init(keySpec);
            byte[] hmac_data = hmac.doFinal(tokdata.getBytes("UTF-8"));
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


    public String getCookieName() {
        return DEFAULT_COOKIE_NAME;
    }
}
