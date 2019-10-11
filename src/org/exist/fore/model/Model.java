/*
 * Copyright (c) 2012. betterFORM Project - http://www.betterform.de
 * Licensed under the terms of BSD License
 */

package org.exist.fore.model;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.fore.XFormsException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import org.exist.fore.model.Instance;

/**
 *
 * @author Joern Turner
 */
public class Model {
    private final static Logger LOGGER = LogManager.getLogger(Model.class);
    private final Element element;
    private List instances;

    /**
     * Creates a new Model object.
     *
     * @param element the DOM Element representing this Model
     */
    public Model(Element element) {
//        super(element);
        this.element = element;
    }

    /**
     * Performs element init.
     *
     */
    public void init(){

//        this.updateSequencer = new UpdateSequencer(this);
        try {
            modelConstruct();
        } catch (XFormsException e) {
            e.printStackTrace();
            // todo: return error log as JSON
        }
    }



    /**
     * Implements <code>xforms-model-construct</code> default action.
     */
    private void modelConstruct() throws XFormsException {
        // load schemas
//        this.schemas = new ArrayList();
//        loadDefaultSchema(this.schemas);

        // The default schema is shared between all models of all forms, and isn't thread safe, so we need synchronization here.
        // We cache the default model bcz. it takes quite some time to construct it.
/*
        synchronized (Model.class) {
            // set datatypes for validation
            getValidator().setDatatypes(getNamedDatatypes(this.schemas));
        }
*/


        // build instances
        this.instances = new ArrayList();

        // todo: move to static method in initializer
        List<Element> instanceElements = getAllInstanceElements();
        int count = instanceElements.size();

        if (count > 0) {
            for (int index = 0; index < count; index++) {
                Element xformsInstance = instanceElements.get(index);
                createInstanceObject(xformsInstance);
            }
        }

        // todo: initialize p3p ?
        // initialize binds and submissions (actions should be initialized already)
//        Initializer.initializeBindElements(this, this.element, new SaxonReferenceFinderImpl());
//        Initializer.initializeActionElements(this, this.element);
//        Initializer.initializeSubmissionElements(this, this.element);

//        rebuild();
//        recalculate();
//        revalidate();
    }



    /**
     * returns the default instance of this model. this is always the first in
     * document order regardless of its id-attribute.
     *
     * @return the default instance of this model
     */
    public Instance getDefaultInstance() {
        if (this.instances != null && this.instances.size() > 0) {
            return (Instance) this.instances.get(0);
        }

        return null;
    }

    /**
     * returns the instance-object for given id.
     *
     * @param id the identifier for instance
     * @return the instance-object for given id.
     */
    public Instance getInstance(String id) {
        if (this.instances == null) {
            return null;
        }
        if ((id == null) || "".equals(id)) {
            return getDefaultInstance();
        }

        for (int index = 0; index < this.instances.size(); index++) {
            Instance instance = (Instance) this.instances.get(index);

            if (id.equals(instance.getId())) {
                return instance;
            }
        }

        return null;
    }

    public List getInstances() {
        return this.instances;
    }


    /**
     * returns this Model object
     *
     * @return this Model object
     */
    public Model getModel() {
        return this;
    }

    /**
     * 7.3.1 The getInstanceDocument() Method.
     * <p/>
     * This method returns a DOM Document that corresponds to the instance data
     * associated with the <code>instance</code> element containing an
     * <code>ID</code> matching the <code>instance-id</code> parameter. If there
     * is no matching instance data, a <code>DOMException</code> is thrown.
     *
     * @param instanceID the instance id.
     * @return the corresponding DOM document.
     * @throws DOMException if there is no matching instance data.
     */
    public Document getInstanceDocument(String instanceID) throws DOMException {
        Instance instance = getInstance(instanceID);
        if (instance == null) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, instanceID);
        }

        return instance.getInstanceDocument();
    }

    public Element getElement(){
        return this.element;
    }

    /**
     * @return
     */
    private List<Element> getAllInstanceElements() {
        List<Element> result = new ArrayList<Element>();
        for (Node it = this.element.getFirstChild(); it != null; it = it.getNextSibling()) {
            if (it.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) it;
            if ("xf-instance".equals(el.getLocalName())) {
                result.add(el);
            }
        }
        return result;
    }

    private void createInstanceObject(Element xformsInstance) throws XFormsException {
//        Instance instance = (Instance) this.container.getElementFactory().createXFormsElement(xformsInstance, this);

        Instance instance = new Instance(xformsInstance, this);
        instance.init();
        this.instances.add(instance);
    }




}

// end of class
