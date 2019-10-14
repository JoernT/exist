/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001-2019 The eXist Project
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

package org.exist.fore.model.bind;

import junit.framework.TestCase;
import org.exist.fore.DOMUtil;
import org.exist.fore.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

public class BindTest extends TestCase {
    private Document doc;
    private Model model;

    @Before
    public void setUp() throws Exception {
        String path = getClass().getResource("bind.html").getPath();
        String fileURI = "file://" + path.substring(0, path.lastIndexOf("bind.html"));

        doc = DOMUtil.parseXmlFile(path,true,false);
//        DOMUtil.prettyPrintDOM(doc);

        model = new Model(this.doc.getDocumentElement());
        model.init();

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInit() {

        assertEquals(4,this.model.getModelBindings().size());
        assertEquals("item1",((Bind)this.model.getModelBindings().get(0)).getElement().getAttribute("id"));
        assertEquals("item2",((Bind)this.model.getModelBindings().get(1)).getElement().getAttribute("id"));
        assertEquals("item3",((Bind)this.model.getModelBindings().get(2)).getElement().getAttribute("id"));

    }

    public void testInitializeModelItems(){
        assertEquals( ((Bind)this.model.getModelBindings().get(0)).getInstanceId(),"default");
        assertEquals( ((Bind)this.model.getModelBindings().get(3)).getInstanceId(),"'other'");

    }
}