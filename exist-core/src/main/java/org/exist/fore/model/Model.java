/*
 * Copyright (c) 2012. betterFORM Project - http://www.betterform.de
 * Licensed under the terms of BSD License
 */

package org.exist.fore.model;


import net.sf.saxon.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.xs.*;
import org.exist.fore.Initializer;
import org.exist.fore.XFormsComputeException;
import org.exist.fore.XFormsElement;
import org.exist.fore.XFormsException;
import org.exist.fore.model.bind.Bind;
import org.exist.fore.model.constraints.MainDependencyGraph;
import org.exist.fore.model.constraints.RefreshView;
import org.exist.fore.model.constraints.Validator;
import org.exist.fore.xpath.*;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 *
 * @author Joern Turner
 */
public class Model {
    private final static Logger LOGGER = LogManager.getLogger(Model.class);
    private final Element element;
    private List instances;
    private List modelBindings;
    private List refreshedItems;
    private String baseURI;
    private Validator validator;
    private boolean ready = false;
    public Map<String, Bind> binds;
    private final Configuration fConfiguration = new Configuration();
    private MainDependencyGraph mainGraph;
    private static int modelItemCounter = 0;

    private List schemas;
    private static XSModel defaultSchema = null;

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

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
        loadDefaultSchema(this.schemas);

        // The default schema is shared between all models of all forms, and isn't thread safe, so we need synchronization here.
        // We cache the default model bcz. it takes quite some time to construct it.
        synchronized (Model.class) {
            // set datatypes for validation
            getValidator().setDatatypes(getNamedDatatypes(this.schemas));
        }


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

        Initializer.initializeBindElements(this, this.element, new BindFunctionReferenceFinderImpl());
//        Initializer.initializeActionElements(this, this.element);
//        Initializer.initializeSubmissionElements(this, this.element);

//        rebuild();
//        recalculate();
//        revalidate();

        this.ready=true;
    }

    /**
     * Generates a model item id.
     *
     * @return a model item id.
     */
    public static String generateModelItemId() {
        // todo: build external id service
        return String.valueOf(++modelItemCounter);
    }

    public boolean isReady(){
        return this.ready;
    }

    public void rebuild() throws XFormsComputeException {

        if (this.modelBindings != null && this.modelBindings.size() > 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(this + " rebuild: creating main dependency graph for " +
                        this.modelBindings.size() + " bind(s)");
            }

            this.mainGraph = new MainDependencyGraph();

            for (int index = 0; index < this.modelBindings.size(); index++) {
                Bind bind = (Bind) this.modelBindings.get(index);
                try {
                    bind.updateXPathContext();
                } catch (XFormsException e) {
                    throw new XFormsComputeException(e.getMessage(), bind.getElement(), bind);

                }
//                this.mainGraph.buildBindGraph(bind, this);
            }

//            this.changed = (Vector) this.mainGraph.getVertices().clone();
        }


        /*
        NodeList bindings = this.element.getElementsByTagName("xf-bind");


        int len = bindings.getLength();


        if (len != 0) {

            Element bind = null;
            for (int i = 0; i < len; i++) {
                bind = (Element) bindings.item(i);

                //evaluate binding expression to determine bound instance nodes
                String bindingExpr;
                if( bind.hasAttribute("ref")){
                    String ref = bind.getAttribute("ref");

                    try {
                        Node n = XPathCache.getInstance().evaluateAsSingleNode((BetterFormXPathContext) getDefaultInstance().getInstanceDocument(),ref);

                    } catch (XFormsException e) {
                        e.printStackTrace();
                    }

                }else if (bind.hasAttribute("set")){

                }else{

                }


            }

        }
*/





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

    public void addRefreshItem(RefreshView changed) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add refreshView " + changed.toString());
        }
        this.refreshedItems.add(changed);
    }


    /**
     * adds a Bind object to this Model
     *
     * @param bind the Bind object to add
     */
    public void addBindElement(Bind bind) {
        if (this.modelBindings == null) {
            this.modelBindings = new ArrayList();
        }

        this.modelBindings.add(bind);
    }

    public void addBind(Bind bind){
        if(this.binds == null){
            this.binds = new HashMap();
        }
        this.binds.put(bind.getId(),bind);
    }



    public Bind lookupBind(String id){
        return this.binds.get(id);
    }


    public Configuration getConfiguration() {
        return fConfiguration;
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


    public List getModelBindings() {
        return modelBindings;
    }

    /**
     * Returns the validator.
     *
     * @return the validator.
     */
    public Validator getValidator() {
        if (this.validator == null) {
            this.validator = new Validator();
            this.validator.setModel(this);
        }

        return this.validator;
    }

    private void loadDefaultSchema(List list) throws XFormsException {
        try {
            synchronized (Model.class) {
                if (this.defaultSchema == null) {
                    // todo: still a hack
                    InputStream stream = Model.class.getResourceAsStream("XFormsDatatypes11.xsd");
                    this.defaultSchema = loadSchema(stream);
                }

                if (this.defaultSchema == null) {
                    throw new NullPointerException("resource not found");
                }
                list.add(this.defaultSchema);
            }
        }
        catch (Exception e) {
            throw new XFormsException("LINK-EXCEPTION: could not load default schema");
        }
    }

    private XSModel loadSchema(InputStream stream) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        LSInput input = new DOMInputImpl();
        input.setByteStream(stream);

        return getSchemaLoader().load(input);
    }

    private XSLoader getSchemaLoader() throws IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        // System.setProperty(DOMImplementationRegistry.PROPERTY,
        // "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        XSImplementation implementation = (XSImplementation) registry.getDOMImplementation("XS-Loader");
        XSLoader loader = implementation.createXSLoader(null);

        DOMConfiguration cfg = loader.getConfig();

        cfg.setParameter("resource-resolver", new LSResourceResolver() {
            public LSInput resolveResource(String type,
                                           String namespaceURI,
                                           String publicId,
                                           String systemId,
                                           String baseURI) {
                LSInput input = new LSInput() {
                    String systemId;

                    public void setSystemId(String systemId) {
                        this.systemId = systemId;
                    }

                    public void setStringData(String s) {
                    }

                    String publicId;

                    public void setPublicId(String publicId) {
                        this.publicId = publicId;
                    }

                    public void setEncoding(String s) {
                    }

                    public void setCharacterStream(Reader reader) {
                    }

                    public void setCertifiedText(boolean flag) {
                    }

                    public void setByteStream(InputStream inputstream) {
                    }

                    String baseURI;

                    public void setBaseURI(String baseURI) {
                        if(baseURI == null || "".equals(baseURI)){
                            baseURI = getBaseURI();
                        }
                        this.baseURI = baseURI;
                    }

                    public String getSystemId() {
                        return this.systemId;
                    }

                    public String getStringData() {
                        return null;
                    }

                    public String getPublicId() {
                        return this.publicId;
                    }

                    public String getEncoding() {
                        return null;
                    }

                    public Reader getCharacterStream() {
                        return null;
                    }

                    public boolean getCertifiedText() {
                        return false;
                    }

                    public InputStream getByteStream() {
                        if(LOGGER.isTraceEnabled()){
                            LOGGER.trace("Schema resource\n\t\t publicId '" + publicId + "'\n\t\t systemId '" + systemId + "' requested");
                        }
                        String pathToSchema = null;
                        if ("http://www.w3.org/MarkUp/SCHEMA/xml-events-attribs-1.xsd".equals(systemId)){
                            pathToSchema = "schema/xml-events-attribs-1.xsd";
                        } else if("http://www.w3.org/2001/XMLSchema.xsd".equals(systemId)) {
                            pathToSchema = "schema/XMLSchema.xsd";
                        } else if("-//W3C//DTD XMLSCHEMA 200102//EN".equals(publicId)){
                            pathToSchema = "schema/XMLSchema.dtd";
                        } else if("datatypes".equals(publicId)){
                            pathToSchema = "schema/datatypes.dtd";
                        } else if("http://www.w3.org/2001/xml.xsd".equals(systemId)){
                            pathToSchema = "schema/xml.xsd";
                        }


                        // LOAD WELL KNOWN SCHEMA
                        if(pathToSchema != null) {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("loading Schema '" +  pathToSchema + "'\n\n");
                            }
                            return Thread.currentThread().getContextClassLoader().getResourceAsStream(pathToSchema);
                        }
                        // LOAD SCHEMA THAT IS NOT(!) YET KNWON TO THE XFORMS PROCESSOR
/*
                        else if (systemId != null && !"".equals(systemId)) {
                            URI schemaURI = new URI(baseURI);
                            schemaURI = schemaURI.resolve(systemId);

                            // ConnectorFactory.getFactory()
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("loading schema resource '" + schemaURI.toString() + "'\n\n");
                            }
                            return ConnectorFactory.getFactory().getHTTPResourceAsStream(schemaURI);

                        }
*/
                        else {
                            LOGGER.error("resource not known '" + systemId + "'\n\n");
                            return null;
                        }

                    }

                    public String getBaseURI() {
                        return this.baseURI;
                    }
                };
                input.setSystemId(systemId);
                input.setBaseURI(baseURI);
                input.setPublicId(publicId);
                return input;
            }
        });
        // END: Patch
        return loader;
    }

    public Map getNamedDatatypes(List schemas) {
        Map datatypes = new HashMap();

        // iterate schemas
        Iterator schemaIterator = schemas.iterator();
        while (schemaIterator.hasNext()) {
            XSModel schema = (XSModel) schemaIterator.next();
            XSNamedMap definitions = schema.getComponents(XSConstants.TYPE_DEFINITION);

            for (int index = 0; index < definitions.getLength(); index++) {
                XSTypeDefinition type = (XSTypeDefinition) definitions.item(index);

                // process named simple types being supported by XForms
                if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE &&
                        !type.getAnonymous() &&
                        getValidator().isSupported(type.getName())) {
                    String name = type.getName();

                    // extract local name
                    int separator = name.indexOf(':');
                    String localName = separator > -1 ? name.substring(separator + 1) : name;

                    // build expanded name
                    String namespaceURI = type.getNamespace();
                    String expandedName = NamespaceResolver.expand(namespaceURI, localName);

                    if (NamespaceConstants.XFORMS_NS.equals(namespaceURI) ||
                            NamespaceConstants.XMLSCHEMA_NS.equals(namespaceURI)) {
                        // register default xforms and schema datatypes without namespace for convenience
                        datatypes.put(localName, type);
                    }

                    // register uniquely named type
                    datatypes.put(expandedName, type);
                }
            }
        }

        return datatypes;
    }

}

// end of class
