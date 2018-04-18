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

import org.eclipse.jetty.deploy.App;

import java.util.HashMap;
import java.util.List;

class RepoAuthCache {
    private static RepoAuthCache instance = null;
    private static final HashMap<String, AppAuth> apps = new HashMap();


    protected RepoAuthCache() {
    }

    public static RepoAuthCache getInstance() {
        if (instance == null) {
            synchronized (RepoAuthCache.class) {
                if (instance == null) {
                    instance = new RepoAuthCache();
                }
            }
        }
        return instance;
    }

    public static List getAppList(String appKey){
        return (List) RepoAuthCache.apps.get(appKey);
    }

    public static void setAuthInfo(String appKey, AppAuth auth){
        RepoAuthCache.apps.put(appKey,auth);
    }

    public static AppAuth getAuthInfo(String appKey){
        AppAuth auth = RepoAuthCache.apps.get(appKey);
        if(auth == null){

        }
        return auth;
    }

    public static void removeApp(String appKey){
        if(RepoAuthCache.apps.containsKey(appKey)){
            RepoAuthCache.apps.remove(appKey);
        }
    }



}
