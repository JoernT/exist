/*
 * Copyright (c) 2012. betterFORM Project - http://www.betterform.de
 * Licensed under the terms of BSD License
 */

package org.exist.fore.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.fore.util.DOMUtil;
import org.exist.fore.XFormsException;
import org.exist.fore.model.constraints.ModelItem;
import org.exist.fore.xpath.BetterFormXPathContext;
import org.exist.fore.xpath.XPathUtil;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

/**
 * Implementation of XForms instance Element.
 *
 * @version $Id: Instance.java 3510 2008-08-31 14:39:56Z lars $
 */
public class Instance {
    private final static Logger LOGGER = LogManager.getLogger(Model.class);
    private final Element element;
    private final Model model;
    private Document instanceDocument = null;
    private Element initialInstance = null;


    /**
     * Creates a new Instance object.
     *
     * @param element the DOM Element of this instance
     * @param model   the owning Model of this instance
     */
    public Instance(Element element, Model model) {
        this.element = element;
        this.model = model;
    }

    public static ModelItem createModelItem(Node node) {
        return null;
    }

    // lifecycle methods

    /**
     * Performs element init.
     *
     */
    public void init() throws XFormsException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(this + " init");
        }

        // load initial instance
        this.initialInstance = createInitialInstance();
        // create instance document
        this.instanceDocument = createInstanceDocument();

        if(this.element.getAttribute("id").equals("")){
            this.element.setAttribute("id","default");
        }
        storeUserObjects();

//        initXPathContext();
    }

    private void storeUserObjects() {
        if(instanceDocument.getDocumentElement() != null){
            instanceDocument.getDocumentElement().setUserData("model",this.model,null);
            instanceDocument.getDocumentElement().setUserData("instance",this,null);
        }
    }

    private Element createInitialInstance() {
        Element child = DOMUtil.getFirstChildElement(this.element);
        if(child != null){
            return child;
        }
        return null;
    }

    /**
     * Returns the instance document.
     *
     * @return the instance document.
     */
    public Document getInstanceDocument() {
        return this.instanceDocument;
    }

/*
    public Document getInitialInstance(){
        return this.initialInstance;
    }
*/

    /**
     * Returns a new created instance document.
     * <p/>
     * If this instance has an original instance, it will be imported into this
     * new document. Otherwise the new document is left empty.
     *
     * @return a new created instance document.
     * @throws org.exist.fore.XFormsException
     */
    private Document createInstanceDocument() throws XFormsException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            Document document = factory.newDocumentBuilder().newDocument();

            if (this.initialInstance != null) {
                Node imported = document.importNode(this.initialInstance.cloneNode(true), true);
                document.appendChild(imported);
            }

            return document;
        } catch (ParserConfigurationException e) {
            throw new XFormsException(e);
        } catch (DOMException e) {
            throw new XFormsException(e);
        }
    }


    public String getId() {
        return this.element.getAttribute("id");
    }

    public ModelItem getModelItem(Node node) {
        return null;
    }

    public BetterFormXPathContext getRootContext() {
        return null;
    }

    public List getInstanceNodeset() {
        String baseURI = model.getBaseURI();
        return XPathUtil.getRootContext(this.instanceDocument,baseURI);
    }
}

// end of class
