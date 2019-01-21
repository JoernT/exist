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

package org.exist.exform;

import org.exist.exform.exception.FormException;
import org.w3c.dom.Node;

/**
 *
 * author: joern turner
 */
public interface FormProcessor {

    /**
     * set the XForms to process. A complete host document embedding XForms syntax (e.g. html/xforms)
     * is expected as input.
     *
     * @param node a DOM Node containing the XForms
     */
    void setXForms(Node node) throws FormException;

    /**
     * fire up form processing.
     *
     * @throws FormException
     */
    void init() throws FormException;

    /**
     * Terminates the form processing. Should perform resource cleanup.
     *
     * @throws FormException if an error occurred during shutdown.
     */

    void shutdown() throws FormException;
}
