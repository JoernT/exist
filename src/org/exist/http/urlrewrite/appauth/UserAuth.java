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


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Formatter;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class UserAuth {
    private static UserAuth instance = null;
    private HashMap users = null;

    private final String HMAC_KEY = "foobar";
    private final String HMAC_ALG = "HmacSHA256";
    private final String TOKEN_SEPARATOR = "|";
    private final int TOKEN_LIFETIME = 300;  // 5min
    private final String DEFAULT_COOKIE_NAME = "AppAuth";


    protected UserAuth() {
        this.users = new HashMap();
    }

    public static UserAuth getInstance() {
        if (instance == null) {
            synchronized (UserAuth.class) {
                if (instance == null) {
                    instance = new UserAuth();
                }
            }
        }
        return instance;
    }

    public void registerUser(String user, String password) {
        LoginDetails details = new LoginDetails(password);
//        UserAuth.users.put()
        this.users.put(user, details);
    }

    public LoginDetails fetchLoginDetails(String username) {
        return (LoginDetails) this.users.get(username);
    }

    public String createToken(String username) {
//        long expires = Instant.now().getEpochSecond() + TOKEN_LIFETIME;
        try {
            String hmac = calcHMAC(username);
            return username + TOKEN_SEPARATOR + hmac;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String validateToken(String token) {
        if (token == null) {
            return null;
        }
        String hmac = null;
        String[] fields = new String[2];
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
        long lastAccessed = details.getLastAccessed();
        if (hmac.equals(fields[1]) && now < lastAccessed + TOKEN_LIFETIME) {
            details.updateLastAccessed();
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


    public String getCookieName() {
        return DEFAULT_COOKIE_NAME;
    }


    public void removeUserAuth(String username) {
        users.remove(username);
    }
}
