package org.jsoup.nodes;

import org.apache.commons.lang.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 Base Node model.

 @author Jonathan Hedley, jonathan@hedley.net */
public abstract class Node {
    Node parentNode;
    final List<Node> childNodes;
    final Attributes attributes;

    /**
     Create a new node.
     */
    protected Node(Attributes attributes) {
        childNodes = new ArrayList<Node>();
        this.attributes = attributes;
    }

    protected Node() {
        this(new Attributes());
    }

    public abstract String nodeName();

    public String attr(String attributeKey) {
        return attributes.get(attributeKey);
    }

    public Node attr(String attributeKey, String attributeValue) {
        attributes.put(attributeKey, attributeValue);
        return this;
    }

    public Node childNode(int index) {
        return childNodes.get(index);
    }

    public List<Node> childNodes() {
        return Collections.unmodifiableList(childNodes);
    }

    public Node parent() {
        return parentNode;
    }

    protected void setParentNode(Node parentNode) {
        if (this.parentNode != null)
            throw new NotImplementedException("Cannot (yet) move nodes in tree"); // TODO: remove from prev node children
        this.parentNode = parentNode;
    }

    public Node nextSibling() {
        // TODO: implement
        return null;
    }

    public Node previousSibling() {
        // TODO: implement
        return null;
    }

    public Attributes getAttributes() {
        return attributes;
        // TODO: probably not have this accessor
    }

    public String toString() {
        return nodeName();
    }
}
