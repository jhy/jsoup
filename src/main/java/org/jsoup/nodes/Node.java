package org.jsoup.nodes;

import java.util.ArrayList;
import java.util.List;

/**
 Base Node model.

 @author Jonathan Hedley, jonathan@hedley.net */
public abstract class Node {
    final Node parentNode;
    final List<Node> childNodes;
    final Attributes attributes;

    /**
     Create a new node.
     @param parentNode This node's parent node. Null indicates this is the root node.
     */
    protected Node(Node parentNode, Attributes attributes) {
        this.parentNode = parentNode;
        childNodes = new ArrayList<Node>();
        this.attributes = attributes;
    }

    public abstract String getNodeName();

    public Node getParentNode() {
        return parentNode;
    }

    public Node getNextSibling() {
        // TODO: implement
        return null;
    }

    public Node getPreviousSibling() {
        // TODO: implement
        return null;
    }
}
