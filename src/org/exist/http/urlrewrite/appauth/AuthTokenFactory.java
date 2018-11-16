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

/*
 * This class provides only two public methods:
 *
 * String createToken(String username)
 *   return token string created from username, expire date and HMAC
 *
 * String validateToken(String token)
 *   return username if HMAC token data can be validated
 *
 * A token consists of 3 fields separated by the TOKEN_SEPARATOR string.
 *   field 1: username for which this token is issued (cleartext)
 *   field 2: expire date in seconds since epoch (cleartext)
 *   field 3: HMAC of fields 1+2, hashed with server side secret key
 *
 * On token creation, calculate current time + token validity period. This is
 * the token expiry date, expressed in seconds since epoch. Join username and
 * expiry by the token separator string, as in "jack|1523912000". This is the
 * raw data that gets passed to HMAC calculation.
 * Calculate HMAC output of raw input hashed with server side secret key and
 * append this output as the third field. The resulting token string would look
 * like "jack|1523912000|0a53e8af...".
 *
 * On token validation, strip off the hmac field, calculate HMAC output of the
 * presented first two fields "jack|1523912000", and compare the resulting
 * HMAC value to the presented HMAC value from the third field.  If the HMAC
 * values do not match, the token is invalid. Otherwise calculate current
 * timestamp. If the current timestamp is later than the presented timestamp
 * in the second field, the token has expired and is therefore invalid.
 * Otherwise the token is valid.
 *
 * This class encapsulates all crypto/HMAC funtionality. For persistent keying,
 * the server side secret is read from a key file in the filesystem. This key
 * file gets created automatically if it does not exist yet.
 */

package org.exist.http.urlrewrite.appauth;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Formatter;
import java.util.regex.Pattern;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class AuthTokenFactory {

    private static AuthTokenFactory instance = null;
    private static SecretKey key = null;
    private static Mac hmac = null;
    private static boolean initialized = false;
    private static String HMAC_KEYFILE = "/tmp/testkey";
    private static String HMAC_ALG = "HmacSHA256";
    public static String TOKEN_SEPARATOR = "|";
    private static int TOKEN_LIFETIME = 300;  // 5min

    private AuthTokenFactory() {
        initialized = initCrypto();
    }

    public static void setLifetime(int ltime) {
        TOKEN_LIFETIME = ltime;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static AuthTokenFactory getInstance() {
        if (instance == null) {
            synchronized (AuthTokenFactory.class) {
                if (instance == null) {
                    instance = new AuthTokenFactory();
                }
            }
        }
        return instance;
    }

    private boolean initCrypto() {
        // try to read HMAC keyfile
        try {
            ObjectInputStream oin = new ObjectInputStream(new FileInputStream(HMAC_KEYFILE));
            try {
                key = (SecretKey) oin.readObject();
            } finally {
                oin.close();
            }
        } catch (FileNotFoundException e) {
            Logger.log("notice", "no HMAC keyfile found");
            // no problem, move on
        } catch (Exception e) {
            Logger.log("error", "ERROR: HMAC keyfile is corrupt: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // create new keyfile if needed
        if (key != null) {
            Logger.log("debug", "HMAC key was read from keyfile");
        } else {
            Logger.log("notice", "creating new HMAC keyfile");
            try {
                // Generate secret key for HmacSHA256
                key = KeyGenerator.getInstance(HMAC_ALG).generateKey();

                // write key to HMAC keyfile
                ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(HMAC_KEYFILE));
                try {
                    oout.writeObject(key);
                } finally {
                    oout.close();
                }
            } catch (NoSuchAlgorithmException e) {
                Logger.log("error", "ERROR: failed to create HMAC key file: " + e.getMessage());
                e.printStackTrace();
                return false;
            } catch (FileNotFoundException e) {
                Logger.log("error", "ERROR: failed to find HMAC key file: " + e.getMessage());
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                Logger.log("error", "ERROR: IOException while accessing keyfile: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        // init HMAC object
        try {
            hmac = Mac.getInstance(HMAC_ALG);
            hmac.init(key);
        } catch (Exception e) {
            Logger.log("error", "ERROR: failed to initialize HMAC crypto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String createToken(String username) {
        long expires = Instant.now().getEpochSecond() + TOKEN_LIFETIME;
        String hmac = new String();
        try {
            hmac = calcHMAC(username, expires);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return username + TOKEN_SEPARATOR + expires + TOKEN_SEPARATOR + hmac;
    }

    public String validateToken(String token, String appname) {
        String[] fields;
        String hmac = null;
        long expires = 0;
        if(token == null){return null;}
        fields = getFields(token);
        expires = Long.parseLong(fields[1]);
        try {
            hmac = calcHMAC(fields[0], expires);
        } catch (UnsupportedEncodingException e) {
            Logger.log("error", "ERROR: UnsupportedEncoding for hmac: " + e.getMessage());
            return null;
        }
        long now = Instant.now().getEpochSecond();
        if (hmac.equals(fields[2]) && now <= expires) {
            //field[0] is the username which is returned here
            return fields[0];
        } else {
            return null;
        }
    }

    public String[] getFields(String token) {
        String[] fields;
        fields = token.split(Pattern.quote(TOKEN_SEPARATOR));
        return fields;
    }

    private String calcHMAC(String username, long tstamp) throws UnsupportedEncodingException {
        String tokdata = username + TOKEN_SEPARATOR + tstamp;
        byte[] hmac_data;

        try {
            hmac_data = hmac.doFinal(tokdata.getBytes("UTF-8"));
        } catch (Exception e) {
            throw e;
        }

        Formatter formatter = new Formatter();
        for (byte b : hmac_data) {
            formatter.format("%02x", b);
        }
        String f = formatter.toString();
        return f;
    }

}
