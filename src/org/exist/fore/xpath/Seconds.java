/*
 * Copyright (c) 2012. betterFORM Project - http://www.betterform.de
 * Licensed under the terms of BSD License
 */

package org.exist.fore.xpath;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.DurationValue;

/**
 * Implementation of 7.9.9 The seconds() Function <p/> This function returns a
 * possibly fractional number of seconds, according to the following rules:
 * 
 * If the string parameter represents a legal lexical xsd:duration, the return
 * value is equal to the number specified in the seconds component plus 60 * the
 * number specified in the minutes component, plus 60 * 60 * the number
 * specified in the hours component, plus 60 * 60 * 24 * the number specified in
 * the days component. The sign of the result will match the sign of the
 * duration. Year and month components, if present, are ignored. Any other input
 * parameter causes a return value of NaN.
 * 
 * @author Nick Van den Bleeken
 * @version $Id$
 */
public class Seconds extends XFormsFunction {

    private static final long serialVersionUID = -166224567432883455L;

    /**
     * Evaluate in a general context
     */
	@Override
    public Item evaluateItem(XPathContext xpathContext) throws XPathException {
		final CharSequence arg = argument[0].evaluateAsString(xpathContext);
		return seconds(arg.toString());
	}

	public Sequence call(final XPathContext context,
						 final Sequence[] arguments) throws XPathException {
		final String arg = arguments[0].head().getStringValue();
		return seconds(arg);
	}

	final DoubleValue seconds(final String strDuration) {
		try {
			final DurationValue argAsDurationValue = (DurationValue) DurationValue.makeDuration(strDuration).asAtomic();

			return new DoubleValue(argAsDurationValue.signum()*( argAsDurationValue.getDays() * 60 * 60 * 24
				+ argAsDurationValue.getHours() * 60 * 60
				+ argAsDurationValue.getMinutes() *60
				+ argAsDurationValue.getSeconds()
				+ (argAsDurationValue.getMicroseconds() / 1000000d)));
		} catch (XPathException e1) {
			return DoubleValue.NaN;
		}
	}
}
