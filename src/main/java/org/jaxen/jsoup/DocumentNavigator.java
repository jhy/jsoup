/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jaxen.jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jaxen.DefaultNavigator;
import org.jaxen.JaxenConstants;
import org.jaxen.NamedAccessNavigator;
import org.jaxen.Navigator;
import org.jaxen.UnsupportedAxisException;
import org.jaxen.XPath;
import org.jaxen.saxpath.SAXPathException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;

/**
 *
 * @author denis.bardadym
 */
public class DocumentNavigator extends DefaultNavigator
        implements NamedAccessNavigator {

    private DocumentNavigator() {
        super();
    }
    private static DocumentNavigator INSTANCE = new DocumentNavigator();

    /**
     * Return a singleton instance of the DocumentNavigator object.
     * @return a Navigator.
     */
    public static Navigator getInstance() {
        return INSTANCE;
    }

    public String getElementNamespaceUri(Object o) {
        return "";
    }

    public String getElementName(Object o) {
        if (isElement(o)) {
            Element element = (Element) o;
            return element.tagName();
        } else {
            return "";
        }
    }

    public String getElementQName(Object o) {
        return getElementName(o);
    }

    public String getAttributeNamespaceUri(Object o) {
        return "";
    }

    public String getAttributeName(Object o) {
        if (o instanceof Attribute) {
            return ((Attribute) o).getKey();
        }
        return "";
    }

    public String getAttributeQName(Object o) {
        return getAttributeName(o);
    }

    public boolean isDocument(Object o) {
        return o instanceof Document;
    }

    public boolean isElement(Object o) {
        return o instanceof Element;
    }

    public boolean isAttribute(Object o) {
        return o instanceof Attribute;
    }

    public boolean isNamespace(Object o) {
        return false;
    }

    public boolean isComment(Object o) {
        return o instanceof Comment;
    }

    public boolean isText(Object o) {
        return o instanceof TextNode;
    }

    public boolean isProcessingInstruction(Object o) {
        return o instanceof XmlDeclaration;
    }

    public String getCommentStringValue(Object o) {
        if (isComment(o)) {
            Comment element = (Comment) o;
            return element.getData();
        } else {
            return "";
        }
    }

    public String getElementStringValue(Object o) {
        if (isElement(o)) {
            Element element = (Element) o;
            return element.toString();

        } else {
            return String.valueOf(o);
        }
    }

    public String getAttributeStringValue(Object o) {
        if (o instanceof Attribute) {
            Attribute attr = (Attribute) o;
            return attr.getValue();
        } else {
            return "";
        }
    }

    public String getNamespaceStringValue(Object o) {
        return getElementStringValue(o);
    }

    public String getTextStringValue(Object o) {
        if (isText(o)) {
            TextNode textNode = (TextNode) o;
            return textNode.getWholeText();
        } else {
            return "";
        }
    }

    public String getNamespacePrefix(Object o) {
        return "";
    }

    public Object getDocument(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return doc;
        } catch (IOException ex) {
        }
        return null;

    }

    public Object getDocumentNode(Object contextNode) {
        if (contextNode instanceof Document) {
            Document source = (Document) contextNode;
            return source.getElementsByTag("html").first();
        } else {
            return contextNode;
        }
    }

    public Object getParentNode(Object contextNode) {
        if (isElement(contextNode)) {
            return ((Element) contextNode).parent();
        } else {
            return null;
        }
    }

    public Object getElementById(Object contextNode, String elementId) {
        if (isElement(contextNode)) {
            Element e = (Element) contextNode;
            return e.getElementById(elementId);
        } else {
            return Collections.emptyList();
        }
    }

    // iteration methods
    /**
     * Returns an iterator for the child Elements of the contextNode
     * Element.
     * @param contextNode the context node Element.
     * @return an Iterator over the child objects of this Element.
     */
    public Iterator getChildAxisIterator(Object contextNode) {
        if (isElement(contextNode)) {
            Element element = (Element) contextNode;
            return element.childNodes().iterator();
        } else {
            return JaxenConstants.EMPTY_ITERATOR;
        }
    }

    /**
     * Returns an iterator over the named child elements of this context
     * node. Comes from NamedAccessNavigator.
     * @param contextNode the context node Element.
     * @param localName the name of the element.
     * @param namespacePrefix not used.
     * @param namespaceURI not used.
     * @return an iterator over the named child elements.
     */
    public Iterator getChildAxisIterator(Object contextNode, String localName,
            String namespacePrefix, String namespaceURI)
            throws UnsupportedAxisException {
        if (contextNode instanceof Element) {
            return ((Element) contextNode).getElementsByTag(localName).iterator();

        } else {
            return JaxenConstants.EMPTY_ITERATOR;
        }
    }

    /**
     * Jericho does not support Namespaces, so returns an empty iterator.
     * @param contextNode the context node Element.
     * @return an empty iterator.
     */
    public Iterator getNamespaceAxisIterator(Object contextNode) {
        return JaxenConstants.EMPTY_ITERATOR;
    }

    /**
     * Returns an iterator over the parent elements of this Context node.
     * @param contextNode the context node Element.
     * @return an iterator over the parent elements of this Element.
     */
    public Iterator getParentAxisIterator(Object contextNode) {
        if (isElement(contextNode)) {
            Element element = (Element) contextNode;


            return element.parents().iterator();
        }
        return JaxenConstants.EMPTY_ITERATOR;

    }

    /**
     * Returns an iterator over the attribute axis of this context node.
     * @param contextNode the context node Element.
     * @return an iterator over the Element's attributes.
     */
    public Iterator getAttributeAxisIterator(Object contextNode) {
        if (isElement(contextNode)) {
            Element element = (Element) contextNode;


            return element.attributes().iterator();
        }
        return JaxenConstants.EMPTY_ITERATOR;
    }

    /**
     * Returns an iterator over the named attributes for this context node.
     * Comes from NamedAccessNavigator.
     * @param contextNode the context node Element.
     * @param localName the name of the attribute.
     * @param namespacePrefix not used.
     * @param namespaceURI not used.
     * @return an iterator over the named attributes of this Element.
     */
    public Iterator getAttributeAxisIterator(Object contextNode,
            String localName, String namespacePrefix, String namespaceURI)
            throws UnsupportedAxisException {
        List namedAttrs = new ArrayList();
        if (contextNode instanceof Element) {

            Attributes attrs = ((Element) contextNode).attributes();
            Iterator ait = attrs.iterator();
            while (ait.hasNext()) {
                Attribute attr = (Attribute) ait.next();
                if (localName.equals(attr.getKey())) {
                    namedAttrs.add(attr);
                }
            }
            return namedAttrs.iterator();
        } else {
            return JaxenConstants.EMPTY_ITERATOR;
        }
    }

    /**
     * Makes sure that we return the correct XPath implementation when
     * called.
     * @param xpath the XPath expression.
     * @return the JerichoXPath object wrapping the expression.
     */
    public XPath parseXPath(String xpath) throws SAXPathException {
        return new JsoupXPath(xpath);
    }

    
}
