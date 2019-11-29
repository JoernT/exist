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

package org.exist.fore.xpath;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ListIterator;
import org.exist.fore.model.Model;

import java.util.Collections;
import java.util.Optional;

public class Bind extends XFormsFunction
{
    private static final long serialVersionUID = -5302742873313974258L;

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     * @return the result of the early evaluation, or the original expression, or potentially
     * a simplified expression
     */
	@Override
    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
		return this;
    }

	/**
	 * Evaluate in a general context
	 */
	@Override
	public SequenceIterator iterate(final XPathContext xpathContext) throws XPathException {
		String bindId = null;
		if (argument.length == 1) {
			final Expression bindIDExpression = argument[0];
			bindId = bindIDExpression.evaluateAsString(xpathContext).toString();
		}
		return bind(xpathContext, bindId);
	}

	public Sequence call(final XPathContext context,
						 final Sequence[] arguments) throws XPathException {
		String bindId = null;
		if(arguments.length == 1) {
			bindId = arguments[0].head().getStringValue();
		} /*else {
			bindId = Optional.empty();
		}*/
		return SequenceTool.toLazySequence(bind(context, bindId));
	}

	private SequenceIterator bind(final XPathContext context, final String bindId) {
		final XPathFunctionContext functionContext = getFunctionContext(context);
		if (functionContext != null) {
			final Model model = functionContext.getXFormsElement().getModel();
//			final org.exist.fore.model.Instance instance;
			final org.exist.fore.model.bind.Bind bind;
//				bind = model.getInstance(bindId.get());
			bind = model.getBind(bindId);

			if (bind != null) {
				return new ListIterator(bind.getNodeset());
			}
		}

		return new ListIterator(Collections.EMPTY_LIST);
	}
}
