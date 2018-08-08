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


import java.util.HashMap;

public class UserAuth {
    private static UserAuth instance = null;
    private HashMap users = null;

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

    public void registerUser(String user, String password, String appName) {
        LoginDetails details = new LoginDetails(appName, password);
        this.users.put(user, details);
    }

    public LoginDetails fetchLoginDetails(String username) {
        return (LoginDetails) this.users.get(username);
    }

    public void removeUserAuth(String username) {
        this.users.remove(username);
    }
}
