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

import java.time.Instant;
import java.util.HashMap;

/* This is a data container class, holding the user password and per-app
 * last login times. This instance is assigned to a username in
 * UserAuth:registerUserDetails()
 *
 * todo: implement expiry for Logindetails so they don't pile up in case users delete their cookies
 */
public class LoginDetails {

    private String pass = null;
    private HashMap lastAccessed = new HashMap();

    public LoginDetails(String appName, String pass){
        this.pass = pass;
        this.lastAccessed.put(appName, Instant.now().getEpochSecond());
    }


    public String getPass() {
        return pass;
    }

    public long getLastAccessed(String appName){
        return (long) this.lastAccessed.get(appName);
    }

    public void updateLastAccessed(String appName){
        this.lastAccessed.put(appName, Instant.now().getEpochSecond());
    }
}
