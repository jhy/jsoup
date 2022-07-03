package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.helper.W3CDom;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.w3c.dom.NodeList;

import java.util.List;

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
    static <T extends Node> List<T> selectXpath(String xpath, Element el, Class<T> nodeType) {
        Validate.notEmpty(xpath);
        Validate.notNull(el);
        Validate.notNull(nodeType);

        W3CDom w3c = new W3CDom().namespaceAware(false);
        org.w3c.dom.Document wDoc = w3c.fromJsoup(el);
        org.w3c.dom.Node contextNode = w3c.contextNode(wDoc);
        NodeList nodeList = w3c.selectXpath(xpath, contextNode);
        return w3c.sourceNodes(nodeList, nodeType);
    }
}
