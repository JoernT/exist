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

import java.util.ArrayList;
import java.util.List;

public class AppAuth {

    private String loginEndpoint;
    private String logoutEndpoint;
    private String loginFailed;
    private List urls = new ArrayList();

    public AppAuth(){
        loginEndpoint = null;
    }

    public String getLoginEndpoint(){
        return this.loginEndpoint;
    }

    public void setLoginEndpoint(String endPoint){
        this.loginEndpoint = endPoint;
        if(!this.urls.contains(endPoint)){
            this.urls.add(endPoint);
        }
    }

    public String getLogoutEndpoint(){
        return this.logoutEndpoint;
    }

    public void setLogoutEndpoint(String endPoint){
        this.logoutEndpoint= endPoint;
        if(!this.urls.contains(endPoint)){
            this.urls.add(endPoint);
        }
    }

    public String getLoginFailed(){
        return this.loginFailed;
    }

    public void setLoginFailed(String loginFailed){
        this.loginFailed=loginFailed;
        if(this.urls.contains(loginFailed)){
            this.urls.add(loginFailed);
        }
    }
    public void setUrls(List urls){
        this.urls=urls;
    }

    public List getWhiteList(){
        return this.urls;
    }


}
