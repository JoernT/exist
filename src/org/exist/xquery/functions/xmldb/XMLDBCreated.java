/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001-2009 The eXist Project
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *  
 *  $Id$
 */
package org.exist.xquery.functions.xmldb;

import org.apache.log4j.Logger;

import java.util.Date;

import org.exist.dom.QName;
import org.exist.xmldb.CollectionImpl;
import org.exist.xmldb.EXistResource;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.DateTimeValue;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 *
 */
public class XMLDBCreated extends XMLDBAbstractCollectionManipulator {
    private static final Logger logger = Logger.getLogger(XMLDBCreated.class);

	public final static FunctionSignature createdSignatures[] = {
        new FunctionSignature(
			new QName("created", XMLDBModule.NAMESPACE_URI, XMLDBModule.PREFIX),
			"Returns the creation date of $resource located in $collection-uri. " +
			"The collection can be passed as a simple collection " +
			"path or an XMLDB URI.",
			new SequenceType[] {
			    new FunctionParameterSequenceType("collection-uri", Type.STRING, Cardinality.EXACTLY_ONE, "the collection"),
			    new FunctionParameterSequenceType("resource", Type.STRING, Cardinality.EXACTLY_ONE, "the resuource")
			},
			new FunctionReturnSequenceType(Type.DATE_TIME, Cardinality.EXACTLY_ONE, "creation date")
        ),
		new FunctionSignature(
			new QName("created", XMLDBModule.NAMESPACE_URI, XMLDBModule.PREFIX),
			"Returns the creation date of $collection-uri. The collection can be passed as a simple collection "
			+ "path or an XMLDB URI.",
			new SequenceType[] {
			    new FunctionParameterSequenceType("collection-uri", Type.STRING, Cardinality.EXACTLY_ONE, "the collection")
			},
			new FunctionReturnSequenceType(Type.DATE_TIME, Cardinality.EXACTLY_ONE, "creation date")
        )
    };
	
	public final static FunctionSignature lastModifiedSignature =
        new FunctionSignature(
			new QName("last-modified", XMLDBModule.NAMESPACE_URI, XMLDBModule.PREFIX),
			"Returns the last-modification date of $resource, located in " +
			"$collection-uri. The collection " +
			"can be passed as a simple collection path or an XMLDB URI.",
			new SequenceType[] {
			    new FunctionParameterSequenceType("collection-uri", Type.ITEM, Cardinality.EXACTLY_ONE, "the collection"),
			    new FunctionParameterSequenceType("resource", Type.STRING, Cardinality.EXACTLY_ONE, "the resource")
			},
			new FunctionReturnSequenceType(Type.DATE_TIME, Cardinality.ZERO_OR_ONE, "last-modification date")
        );
	
	public XMLDBCreated(XQueryContext context, FunctionSignature signature) {
		super(context, signature);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
    public Sequence evalWithCollection(Collection collection, Sequence[] args, Sequence contextSequence)
	throws XPathException {

	logger.info("Entering " + XMLDBModule.PREFIX + ":" + getName().getLocalName());
	try {
	    Date date;
	    if(getSignature().getArgumentCount() == 1) {
                date = ((CollectionImpl)collection).getCreationTime();
	    } else {
                Resource resource = collection.getResource(args[1].getStringValue());
                
                if(resource == null) {
		    logger.info("Exiting " + XMLDBModule.PREFIX + ":" + getName().getLocalName());
                    return Sequence.EMPTY_SEQUENCE;
                }
                
                if(isCalledAs("last-modified"))
		    date = ((EXistResource)resource).getLastModificationTime();
                else
		    date = ((EXistResource)resource).getCreationTime();
            }

	    logger.info("Exiting " + XMLDBModule.PREFIX + ":" + getName().getLocalName());
	    return new DateTimeValue(date);

	} catch(XMLDBException e) {
	    logger.error("Failed to retrieve creation date or modification time of specified resource or creation date of collection");
	    logger.info("Exiting " + XMLDBModule.PREFIX + ":" + getName().getLocalName());

	    throw new XPathException(this, "Failed to retrieve creation date: " + e.getMessage(), e);
	}
    }

}
