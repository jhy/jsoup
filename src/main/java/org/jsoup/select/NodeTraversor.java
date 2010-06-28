package org.jsoup.select;

import org.jsoup.nodes.Node;

/**
 * Breadth first node traversor.
 */
public class NodeTraversor {
    private NodeVisitor visitor;

    public NodeTraversor(NodeVisitor visitor) {
        this.visitor = visitor;
    }

    public void traverse(Node root) {
        Node node = root;
        int depth = 0;
        
        while (node != null) {
            visitor.head(node, depth);
            if (node.childNodes().size() > 0) {
                node = node.childNode(0);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    visitor.tail(node, depth);
                    node = node.parent();
                    depth--;
                }
                visitor.tail(node, depth);
                if (node == root)
                    break;
                node = node.nextSibling();
            }
        }
    }
}
