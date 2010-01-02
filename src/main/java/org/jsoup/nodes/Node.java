package org.jsoup.nodes;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;

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
        List<Node> siblings = parentNode.childNodes;
        Integer index = indexInList(this, siblings);
        Validate.notNull(index);
        if (siblings.size() > index+1)
            return siblings.get(index+1);
        else
            return null;
    }

    public Node previousSibling() {
        List<Node> siblings = parentNode.childNodes;
        Integer index = indexInList(this, siblings);
        Validate.notNull(index);
        if (index > 0)
            return siblings.get(index-1);
        else
            return null;
    }

    protected static <N extends Node> Integer indexInList(N search, List<N> nodes) {
        Validate.notNull(search);
        Validate.notNull(nodes);

        for (int i = 0; i < nodes.size(); i++) {
            N node = nodes.get(i);
            if (node.equals(search))
                return i;
        }
        return null;
    }

    public Attributes getAttributes() {
        return attributes;
        // TODO: probably not have this accessor
    }

    public abstract String html();

    public String toString() {
        return nodeName();
    }
}
