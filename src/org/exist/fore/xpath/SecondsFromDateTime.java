/*
 * Copyright (c) 2012. betterFORM Project - http://www.betterform.de
 * Licensed under the terms of BSD License
 */

package org.exist.fore.xpath;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.DoubleValue;

/**
 * Implementation of 7.9.6 The seconds-from-dateTime() Function <p/> This
 * function returns a possibly fractional number of seconds, according to the
 * following rules:
 * 
 * If the string parameter represents a legal lexical xsd:dateTime, the return
 * value is equal to the number of seconds difference between the specified
 * dateTime (normalized to UTC) and 1970-01-01T00:00:00Z. If no time zone is
 * specified, UTC is used. Any other input string parameter causes a return
 * value of NaN. This function does not support leap seconds.
 * 
 * @author Nick Van den Bleeken
 * @version $Id$
 */
public class SecondsFromDateTime extends XFormsFunction {

    private static final long serialVersionUID = -166224567432883455L;

    /**
     * Evaluate in a general context
     */
	@Override
    public Item evaluateItem(final XPathContext xpathContext) throws XPathException {
		final CharSequence dateTime = argument[0].evaluateAsString(xpathContext);
		return secondsFromDateTime(dateTime.toString());
    }

	public Sequence call(final XPathContext context,
						 final Sequence[] arguments) throws XPathException {
		final String dateTime = arguments[0].head().getStringValue();
		return secondsFromDateTime(dateTime);
	}

	final DoubleValue secondsFromDateTime(final String dateTime) {
		try {
			final DateTimeValue argAsDateTime = (DateTimeValue) DateTimeValue.makeDateTimeValue(dateTime, new ConversionRules()).asAtomic();
			return new DoubleValue(argAsDateTime.getCalendar().getTimeInMillis() / 1000d);
		} catch (final XPathException e1) {
			return DoubleValue.NaN;
		}
	}
}