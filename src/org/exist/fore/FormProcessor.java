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

package org.exist.fore;

import oracle.jvm.hotspot.jfr.ThreadStates;
import org.exist.fore.exception.FormException;
import org.exist.fore.model.Model;
import org.exist.fore.model.States;
import org.w3c.dom.Node;

/**
 *
 * author: joern turner
 */
public interface FormProcessor {


    /**
     * fire up form processing.
     *
     * @throws FormException
     */
    States init(Model model) throws FormException;


    /**
     * batch updating instance data with a Changelog.
     *
     * @param model the Model to update
     * @param log the log to process
     * @return a States object representing states resulting from data manipulations
     */
    States update(Model model, ChangeLog log);


    /**
     * Terminates the form processing. Should perform resource cleanup.
     *
     * @throws FormException if an error occurred during shutdown.
     */

    void shutdown() throws FormException;
}
