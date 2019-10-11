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

package org.exist.fore.model;

import junit.framework.TestCase;
import org.exist.atom.util.DOM;
import org.exist.fore.DOMComparator;
import org.exist.fore.DOMUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class ModelTest extends TestCase {
    private Document doc;
    private Model model;

    @Before
    public void setUp() throws Exception {
        String path = getClass().getResource("simple.html").getPath();
        String fileURI = "file://" + path.substring(0, path.lastIndexOf("simple.html"));

        doc = DOMUtil.parseXmlFile(path,true,false);
//        DOMUtil.prettyPrintDOM(doc);

        model = new Model(this.doc.getDocumentElement());
        model.init();
    }

    /*

    @After
    public void tearDown() throws Exception {
    }
*/

    @Test
    public void testInit() throws IOException, SAXException, ParserConfigurationException {

        assertNotNull(model);
        assertEquals(this.model.getElement().getLocalName(),"xf-model");

        List instances = model.getInstances();
        assertNotNull(instances);
        assertEquals(this.model.getInstances().size(),1);
        assertNotNull(this.model.getInstanceDocument("default"));




    }


    public void testGetDefaultInstance() throws IOException, SAXException, ParserConfigurationException {
        assertNotNull(this.model.getDefaultInstance());
        assertEquals("default", this.model.getDefaultInstance().getId());
    }

    public void testGetInstance() throws IOException, SAXException, ParserConfigurationException {
        assertNotNull(this.model.getInstance("default"));
        assertEquals("default", this.model.getInstance("default").getId());
        assertNotNull(this.model.getInstance(""));
        assertNull(this.model.getInstance("foo"));
    }


    protected DOMComparator getComparator() {
        DOMComparator comparator = new DOMComparator();
        comparator.setIgnoreNamespaceDeclarations(true);
        comparator.setIgnoreWhitespace(true);
        comparator.setIgnoreComments(true);
        comparator.setErrorHandler(new DOMComparator.SystemErrorHandler());

        return comparator;
    }

}