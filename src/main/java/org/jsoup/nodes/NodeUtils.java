package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.helper.W3CDom;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Internal helpers for Nodes, to keep the actual node APIs relatively clean. A jsoup internal class, so don't use it as
 * there is no contract API).
 */
final class NodeUtils {
    /**
     * Get the output setting for this node,  or if this node has no document (or parent), retrieve the default output
     * settings
     */
    static Document.OutputSettings outputSettings(Node node) {
        Document owner = node.ownerDocument();
        return owner != null ? owner.outputSettings() : (new Document("")).outputSettings();
    }

    /**
     * Get the parser that was used to make this node, or the default HTML parser if it has no parent.
     */
    static Parser parser(Node node) {
        Document doc = node.ownerDocument();
        return doc != null && doc.parser() != null ? doc.parser() : new Parser(new HtmlTreeBuilder());
    }

    /**
     This impl works by compiling the input xpath expression, and then evaluating it against a W3C Document converted
     from the original jsoup element. The original jsoup elements are then fetched from the w3c doc user data (where we
     stashed them during conversion). This process could potentially be optimized by transpiling the compiled xpath
     expression to a jsoup Evaluator when there's 1:1 support, thus saving the W3C document conversion stage.
     */
    static Elements selectXpath(String xpath, Element el) {
        Validate.notEmpty(xpath);
        Validate.notNull(el);

        NodeList nodeList;
        try {
            XPathExpression expression = XPathFactory.newInstance().newXPath().compile(xpath);
            W3CDom w3c = new W3CDom();
            org.w3c.dom.Document wDoc = w3c.fromJsoup(el);
            nodeList = (NodeList) expression.evaluate(wDoc, XPathConstants.NODESET); // love the strong typing here /s
            Validate.notNull(nodeList);
        } catch (XPathExpressionException e) {
            throw new Selector.SelectorParseException("Could not evaluate XPath query [%s]: %s", xpath, e.getMessage());
        }

        Elements els = new Elements();
        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node node = nodeList.item(i);
            Object source = node.getUserData(W3CDom.SourceProperty);
            if (source instanceof Element)
                els.add((Element) source);
        }

        return els;
    }
}
