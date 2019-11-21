/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001-2017 The eXist Project
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

package org.exist.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * w3c.Dom utility methods
 *
 * @author Joern Turner
 */
public class DOMUtil {

    /**
     * get first child element with specific nodeName
     *
     * @param start the parent node to start searching
     * @param localname  the nodeName of the searched node
     * @return the found element or null
     */
    public static Element getChildElementByLocalName(Node start, String localname) {
        NodeList nl = null;

        if (start.getNodeType() == Node.DOCUMENT_NODE) {
            nl = ((Document) start).getDocumentElement().getChildNodes();
        } else {
            nl = start.getChildNodes();
        }

        int len = nl.getLength();
        Node n = null;

        for (int i = 0; i < len; i++) {
            n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getLocalName().equals(localname)) {
                    return (Element) n;
                }
            }
        }

        return null;
    }

    /**
     * returns a java.util.List of Elements which are children of the start Element.
     */
    public static List getChildElements(Node start) {
        List l = new ArrayList();
        NodeList nl = start.getChildNodes();
        int len = nl.getLength();
        Node n = null;

        for (int i = 0; i < len; i++) {
            n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                l.add(n);
            }
        }

        return l;
    }

    /**
     * gets the first child of a node which is a text or cdata node.
     *
     * @param: the Element from which to get the textnode
     */
    public static Node getTextNode(Node start) {
        Node n = null;

        start.normalize();

        NodeList nl;
        if (start.getNodeType() == Node.DOCUMENT_NODE) {
            nl = ((Document) start).getDocumentElement().getChildNodes();
        } else {
            nl = start.getChildNodes();
        }

        int len = nl.getLength();

        if (len == 0) {
            return null;
        }

        for (int i = 0; i < len; i++) {
            n = nl.item(i);

            if (n.getNodeType() == Node.TEXT_NODE) {
                return n;
            } else if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                return n;
            }
        }

        return null;
    }

    /**
     * returns the Text-Node child of Node 'start' as String. If no TextNode exists, an empty string is returned.
     */
    public static String getTextNodeAsString(Node start) {
        Node txt = getTextNode(start);

        if (txt != null) {
            return txt.getNodeValue();
        }

        return "";
    }

}
