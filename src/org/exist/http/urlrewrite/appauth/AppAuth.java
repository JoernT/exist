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

import java.util.ArrayList;
import java.util.List;

public class AppAuth {


    private String loginEndpoint = null;
    private String logoutEndpoint = null;
    private String loginFailedEndpoint = null;
    private String logoutRedirectEndpoint = null;
    private List   whitelist = new ArrayList();
    private String userName = null;
    private int    lifeTime =  900;  // defaults to 15min

    //public AppAuth() {}

    public String getLoginEndpoint() {
        return this.loginEndpoint;
    }

    public void setLoginEndpoint(String endPoint) {
        this.loginEndpoint = endPoint;
        if (!this.whitelist.contains(endPoint)) {
            this.whitelist.add(endPoint);
        }
    }

    public String getLogoutEndpoint() {
        return this.logoutEndpoint;
    }

    public void setLogoutEndpoint(String endPoint) {
        this.logoutEndpoint = endPoint;
        if (!this.whitelist.contains(endPoint)) {
            this.whitelist.add(endPoint);
        }
    }

    public String getLogoutRedirect(){
        return this.logoutRedirectEndpoint;
    }

    public void setLogoutRedirect(String logoutRedirect) {
        this.logoutRedirectEndpoint = logoutRedirect;
        if (!this.whitelist.contains(logoutRedirect)) {
            this.whitelist.add(logoutRedirect);
        }
    }

    public String getLoginFailed() {
        return this.loginFailedEndpoint;
    }

    public void setLoginFailed(String loginFailed) {
        this.loginFailedEndpoint = loginFailed;
        if (!this.whitelist.contains(loginFailed)) {
            this.whitelist.add(loginFailed);
        }
    }

    public void setWhiteList(List urls) {
        this.whitelist = urls;
    }

    public List getWhiteList() {
        return this.whitelist;
    }

    public int getLifeTime() {
        return this.lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    public String getUserName() {
        return userName;
    }
}
