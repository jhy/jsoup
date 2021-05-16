package org.jsoup.nodes;

import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.Parser;

import javax.print.Doc;

/**
 * Internal helpers for Nodes, to keep the actual node APIs relatively clean. A jsoup internal class, so don't use it as
 * there is no contract API).
 */
final class NodeUtils {
    /**
     * Get the output setting for this node,  or if this node has no document (or parent), retrieve the default output
     * settings, or if this node is an instance of Element, check if there is a designated output setting
     */
    static Document.OutputSettings outputSettings(Node node) {
        Document owner = node.ownerDocument();
        if (owner == null && node instanceof Element)
        {
            Document.OutputSettings out = ((Element) node).getOutputSettings();
            return out != null ? out : (new Document("")).outputSettings();
        }
        else
        {
            return owner != null ? owner.outputSettings() : (new Document("")).outputSettings();
        }
    }

    /**
     * Get the parser that was used to make this node, or the default HTML parser if it has no parent.
     */
    static Parser parser(Node node) {
        Document doc = node.ownerDocument();
        return doc != null && doc.parser() != null ? doc.parser() : new Parser(new HtmlTreeBuilder());
    }
}
