package org.jsoup.nodes;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;

import java.net.MalformedURLException;
import java.net.URL;
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
    final String baseUri;

    /**
     Create a new node.
     */
    protected Node(String baseUri, Attributes attributes) {
        Validate.notNull(baseUri);
        Validate.notNull(attributes);
        
        childNodes = new ArrayList<Node>();
        this.baseUri = baseUri.trim();
        this.attributes = attributes;
    }

    protected Node(String baseUri) {
        this(baseUri, new Attributes());
    }

    public abstract String nodeName();

    public String attr(String attributeKey) {
        String value = attributes.get(attributeKey);
        return value == null ? "" : value;
    }

    public Node attr(String attributeKey, String attributeValue) {
        attributes.put(attributeKey, attributeValue);
        return this;
    }

    public boolean hasAttr(String attributeKey) {
        Validate.notNull(attributeKey);
        return attributes.hasKey(attributeKey);
    }

    public String baseUri() {
        return baseUri;
    }

    public String absUrl(String attribute) {
        Validate.notEmpty(attribute);

        String relUrl = attr(attribute);
        if (baseUri.isEmpty()) {
            return relUrl; // nothing to make absolute with
        } else {
            URL base;
            try {
                try {
                    base = new URL(baseUri);
                } catch (MalformedURLException e) {
                    // the base is unsuitable, but the attribute may be abs, so try that
                    URL abs = new URL(relUrl);
                    return abs.toExternalForm();
                }
                URL abs = new URL(base, relUrl);
                return abs.toExternalForm();
            } catch (MalformedURLException e) {
                return "";
            }
        }
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

    public abstract String outerHtml();

    public String toString() {
        return outerHtml();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // todo: have nodes hold a child index, compare against that and parent (not children)
        return false;
    }

    @Override
    public int hashCode() {
        int result = parentNode != null ? parentNode.hashCode() : 0;
        // not children, or will block stack as they go back up to parent)
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }
}
