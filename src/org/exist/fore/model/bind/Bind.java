/*
 * Copyright (c) 2012. betterFORM Project - http://www.betterform.de
 * Licensed under the terms of BSD License
 */

package org.exist.fore.model.bind;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exist.fore.XFormsElement;
import org.exist.fore.XFormsException;
import org.exist.fore.model.Instance;
import org.exist.fore.model.Model;
import org.exist.fore.xpath.XPathParseUtil;
import org.w3c.dom.Element;

import java.util.*;


/**
 * Implementation of XForms Model Bind Element.
 *
 * @version $Id: Bind.java 3479 2008-08-19 10:44:53Z joern $
 */
public class Bind extends XFormsElement {

    private static Log LOGGER = LogFactory.getLog(Bind.class);

    private String locationPath;
    private String instanceId = "default";

    private String type;
    private String readonly;
    private String required;
    private String relevant;
    private String calculate;
    private String constraint;
    private List constraints;
    private String p3ptype;
    private Map<String,String> customMIPs;

//    private XPathReferenceFinder referenceFinder;
    private Set readonlyReferences;
    private Set requiredReferences;
    private Set relevantReferences;
    private Set calculateReferences;
    private Set constraintReferences;
    private HashMap<String, Set> customMIPReferences;

    protected List nodeset;

    private static final short TYPE = 0;
    private static final short READONLY = 1;
    private static final short REQUIRED = 2;
    private static final short RELEVANT = 3;
    private static final short CONSTRAINT = 4;
    private static final short CALCULATE = 5;

    private static final String COMBINE_NOT_SUPPORTED = null;
    private static final String COMBINE_ALL ="and";
    private static final String COMBINE_ONE ="or";

    private static final String TYPE_COMBINE=COMBINE_ALL;
    private static final String CONSTRAINT_COMBINE=COMBINE_ALL;
    private static final String RELEVANT_COMBINE =COMBINE_ALL;

    private static final String REQUIRED_COMBINE=COMBINE_ONE;
    private static final String READONLY_COMBINE=COMBINE_ONE;


    /**
     * Creates a new Bind object.
     *
     * @param element the DOM Element annotated by this object
     * @param model   the parent Model object
     */
    public Bind(Element element, Model model) {
        super(element, model);
        this.constraints = new ArrayList();
        // register with model
        getModel().addBindElement(this);
    }

    /**
     * Performs element init.
     *
     * @throws XFormsException if any error occurred during init.
     */
    public void init() throws XFormsException {
        if(this.element.hasAttribute("id")){
            this.id = this.element.getAttribute("id");
        }
        model.addBind(this);
        initializeModelItems();
//        Initializer.initializeBindElements(getModel(), getElement(), this.referenceFinder);
    }

    /**
     * Initializes all bound model items.
     *
     * @throws XFormsException if any error occured during model item init.
     */
    protected void initializeModelItems() throws XFormsException {
        String bindingExpression = getBindingExpression();
        Instance instance;

        // bindingExpression starts with 'instance' function
        if(bindingExpression != null && bindingExpression.startsWith("instance(")){
            this.instanceId = XPathParseUtil.getInstanceParameter(bindingExpression);
        }
        // else - even if there's no binding expression at all the default instance is always the default ;)
        instance = getModel().getInstance(instanceId);



/*
        List nodeset = getNodeset();
        if(nodeset != null && nodeset.size() > 0) {
            Iterator iterator = instance.iterateModelItems(nodeset, false);
            if (iterator != null) {
                ModelItem modelItem;
                while (iterator.hasNext()) {
                    modelItem = (ModelItem) iterator.next();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(this + " init: model item for " + DOMUtil.getCanonicalPath((Node) modelItem.getNode()));
                    }

                    // 4.2.1 - 4.b applying model item properties to each node
                    initializeModelItemProperties(modelItem);


                }
            }
        }
*/
    }



    /**
     * Returns the binding expression.
     *
     * @return the binding expression.
     */
    public String getBindingExpression() {
        if(this.element.hasAttribute("ref")){
            return this.element.getAttribute("ref");
        }
        return this.element.getAttribute("set");
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void dispose() throws XFormsException {
    }

    @Override
    protected Log getLogger() {
        return LOGGER;
    }


}

// end of class
