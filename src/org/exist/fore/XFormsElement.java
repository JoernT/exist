/*
 * Copyright (c) 2012. betterFORM Project - http://www.betterform.de
 * Licensed under the terms of BSD License
 */

package org.exist.fore;

import org.exist.fore.DOMUtil;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.logging.Log;
import org.exist.fore.model.Model;
import org.exist.fore.xpath.XPathFunctionContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Superclass for all XForms elements. This includes either all elements from
 * the XForms namespace and all bound elements which may be from foreign
 * namespaces but wear XForms binding attributes. Custom elements also extend
 * this class.
 *
 * @author Joern Turner
 * @author Ulrich Nicolas Liss&eacute;
 * @version $Id: XFormsElement.java 3483 2008-08-20 10:16:24Z joern $
 */
public abstract class XFormsElement {

    protected final XPathFunctionContext xpathFunctionContext;
    /**
     * the annotated DOM Element
     */
    protected Element element = null;

    /**
     * the Model object of this XFormsElement
     */
    protected Model model = null;

    /**
     * the id of this Element
     */
    protected String id;

    /**
     * the original id of this Element (when repeated)
     */
//    protected String originalId;

    /**
     * the xforms prefix used in this Document
     */
//    protected String xformsPrefix = null;
//    protected final Map prefixMapping;



    /**
     * Creates a new XFormsElement object.
     *
     * @param element the DOM Element annotated by this object
     */
    public XFormsElement(Element element) {
        this.element = element;

        xpathFunctionContext = new XPathFunctionContext(this);

    }

    /**
     * Creates a new XFormsElement object.
     *
     * @param element the DOM Element annotated by this object
     * @param model   the Model object of this XFormsElement
     */
    public XFormsElement(Element element, Model model) {
        this(element);
        this.model = model;
    }

    // lifecycle methods

    /**
     * Performs element init.
     *
     * @throws XFormsException if any error occurred during init.
     */
    public abstract void init() throws XFormsException;

    /**
     * Performs element disposal.
     *
     * @throws XFormsException if any error occurred during disposal.
     */
    public abstract void dispose() throws XFormsException;


    /**
     * returns the Container object of this Element.
     *
     * @return Container object of this Element
     */
    public Model getContainerObject() {
        return (Model)this.element.getOwnerDocument().getDocumentElement().getUserData("");
    }

    /**
     * Returns the DOM element of this element.
     *
     * @return the DOM element of this element.
     */
    public Element getElement() {
        return this.element;
    }

    // member access methods

    /**
     * Returns the global id of this element.
     *
     * @return the global id of this element.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the context model of this element.
     *
     * @return the context model of this element.
     */
    public Model getModel() {
        return this.model;
    }

    /**
     * returns the parent XFormsElement object of the DOM parent Node if any or
     * null otherwise.
     *
     * @return the parent XFormsElement object of the DOM parent Node if any or
     *         null otherwise.
     */
/*
    public XFormsElement getParentObject() {
        return (XFormsElement) this.element.getParentNode().getUserData("");
    }
*/



    /**
     * @return the the XPathFunctionContext for this element.
     */
/*
    public XPathFunctionContext getXPathFunctionContext() {
        return xpathFunctionContext;
    }
*/

    // id handling



    // standard methods

    /**
     * Check wether this object and the specified object are equal.
     *
     * @param object the object in question.
     * @return <code>true</code> if this object and the specified object are
     *         equal, <code>false</code> otherwise.
     */
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        if (object == this) {
            return true;
        }

        if (!(object instanceof XFormsElement)) {
            return false;
        }

        return ((XFormsElement) object).getId().equals(getId());
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    public String toString() {
        return DOMUtil.getCanonicalPath(getElement()) + "/@id[.='" + getId() + "']";
    }

    /**
     * Returns the logger object.
     *
     * @return the logger object.
     */
     protected abstract Log getLogger();





}

// end of class
