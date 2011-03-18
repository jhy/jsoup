/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jaxen.jsoup;

import java.util.Collections;
import java.util.List;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.Navigator;

/**
 *
 * @author denis.bardadym
 */
public class JsoupXPath extends BaseXPath {

    public JsoupXPath(String xpathExpr) throws JaxenException {
        super(xpathExpr, DocumentNavigator.getInstance());
    }

    public JsoupXPath(String xpath, Navigator navigator) throws JaxenException {
        super(xpath, navigator);
    }

    public String valueOf(Object context) throws JaxenException {
        return stringValueOf(context);
    }

    public boolean booleanValueOf(Object context) throws JaxenException {
        String result = stringValueOf(context);
        return Boolean.parseBoolean(result);
    }

    public Number numberValueOf(Object context) throws JaxenException {
        String result = stringValueOf(context);

        try {
            return Double.valueOf(result);
        } catch (NumberFormatException e) {
            throw new JaxenException(e.getMessage(), e);
        }
    }

    public String stringValueOf(Object context) throws JaxenException {
        Object result = evaluate(context);
        if (result instanceof String) {
            return (String) result;
        } else {
            throw new JaxenException("Cannot return string");
        }
    }

    public Object selectSingleNode(Object context) throws JaxenException {
        List nodes = selectNodes(context);
        if (nodes.size() > 0) {
            return nodes.get(0);
        } else {
            return null;
        }
    }

   
}
