package org.jsoup.nodes;

import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 The base, abstract Node model. Elements, Documents, Comments etc are all Node instances.

 @author Jonathan Hedley, jonathan@hedley.net */
public abstract class Node {
    Node parentNode;
    List<Node> childNodes;
    Attributes attributes;
    String baseUri;
    int siblingIndex;

    /**
     Create a new Node.
     @param baseUri base URI
     @param attributes attributes (not null, but may be empty)
     */
    protected Node(String baseUri, Attributes attributes) {
        Validate.notNull(baseUri);
        Validate.notNull(attributes);
        
        childNodes = new ArrayList<Node>(4);
        this.baseUri = baseUri.trim();
        this.attributes = attributes;
    }

    protected Node(String baseUri) {
        this(baseUri, new Attributes());
    }

    /**
     * Default constructor. Doesn't setup base uri, children, or attributes; use with caution.
     */
    protected Node() {
        childNodes = Collections.EMPTY_LIST;
        attributes = null;
    }

    /**
     Get the node name of this node. Use for debugging purposes and not logic switching (for that, use instanceof).
     @return node name
     */
    public abstract String nodeName();

    /**
     * Get an attribute's value by its key.
     * <p/>
     * To get an absolute URL from an attribute that may be a relative URL, prefix the key with <code><b>abs</b></code>,
     * which is a shortcut to the {@link #absUrl} method.
     * E.g.: <blockquote><code>String url = a.attr("abs:href");</code></blockquote>
     * @param attributeKey The attribute key.
     * @return The attribute, or empty string if not present (to avoid nulls).
     * @see #attributes()
     * @see #hasAttr(String)
     * @see #absUrl(String)
     */
    public String attr(String attributeKey) {
        Validate.notNull(attributeKey);

        if (hasAttr(attributeKey))
            return attributes.get(attributeKey);
        else if (attributeKey.toLowerCase().startsWith("abs:"))
            return absUrl(attributeKey.substring("abs:".length()));
        else return "";
    }

    /**
     * Get all of the element's attributes.
     * @return attributes (which implements iterable, in same order as presented in original HTML).
     */
    public Attributes attributes() {
        return attributes;
    }

    /**
     * Set an attribute (key=value). If the attribute already exists, it is replaced.
     * @param attributeKey The attribute key.
     * @param attributeValue The attribute value.
     * @return this (for chaining)
     */
    public Node attr(String attributeKey, String attributeValue) {
        attributes.put(attributeKey, attributeValue);
        return this;
    }

    /**
     * Test if this element has an attribute.
     * @param attributeKey The attribute key to check.
     * @return true if the attribute exists, false if not.
     */
    public boolean hasAttr(String attributeKey) {
        Validate.notNull(attributeKey);
        return attributes.hasKey(attributeKey);
    }

    /**
     * Remove an attribute from this element.
     * @param attributeKey The attribute to remove.
     * @return this (for chaining)
     */
    public Node removeAttr(String attributeKey) {
        Validate.notNull(attributeKey);
        attributes.remove(attributeKey);
        return this;
    }

    /**
     Get the base URI of this node.
     @return base URI
     */
    public String baseUri() {
        return baseUri;
    }

    /**
     Update the base URI of this node.
     @param baseUri base URI to set
     */
    public void setBaseUri(String baseUri) {
        Validate.notNull(baseUri);
        this.baseUri = baseUri;
    }

    /**
     * Get an absolute URL from a URL attribute that may be relative (i.e. an <code>&lt;a href></code> or
     * <code>&lt;img src></code>).
     * <p/>
     * E.g.: <code>String absUrl = linkEl.absUrl("href");</code>
     * <p/>
     * If the attribute value is already absolute (i.e. it starts with a protocol, like
     * <code>http://</code> or <code>https://</code> etc), and it successfully parses as a URL, the attribute is
     * returned directly. Otherwise, it is treated as a URL relative to the element's {@link #baseUri}, and made
     * absolute using that.
     * <p/>
     * As an alternate, you can use the {@link #attr} method with the <code>abs:</code> prefix, e.g.:
     * <code>String absUrl = linkEl.attr("abs:href");</code>
     *
     * @param attributeKey The attribute key
     * @return An absolute URL if one could be made, or an empty string (not null) if the attribute was missing or
     * could not be made successfully into a URL.
     * @see #attr
     * @see java.net.URL#URL(java.net.URL, String)
     */
    public String absUrl(String attributeKey) {
        Validate.notEmpty(attributeKey);

        String relUrl = attr(attributeKey);
        if (!hasAttr(attributeKey)) {
            return ""; // nothing to make absolute with
        } else {
            URL base;
            try {
                try {
                    base = new URL(baseUri);
                } catch (MalformedURLException e) {
                    // the base is unsuitable, but the attribute may be abs on its own, so try that
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

    /**
     Get a child node by index
     @param index index of child node
     @return the child node at this index.
     */
    public Node childNode(int index) {
        return childNodes.get(index);
    }

    /**
     Get this node's children. Presented as an unmodifiable list: new children can not be added, but the child nodes
     themselves can be manipulated.
     @return list of children. If no children, returns an empty list.
     */
    public List<Node> childNodes() {
        return Collections.unmodifiableList(childNodes);
    }
    
    protected Node[] childNodesAsArray() {
        return childNodes.toArray(new Node[childNodes().size()]);
    }

    /**
     Gets this node's parent node.
     @return parent node; or null if no parent.
     */
    public Node parent() {
        return parentNode;
    }
    
    /**
     * Gets the Document associated with this Node. 
     * @return the Document associated with this Node, or null if there is no such Document.
     */
    public Document ownerDocument() {
        if (this instanceof Document)
            return (Document) this;
        else if (parentNode == null)
            return null;
        else
            return parentNode.ownerDocument();
    }
    
    /**
     * Remove (delete) this node from the DOM tree. If this node has children, they are also removed.
     */
    public void remove() {
        Validate.notNull(parentNode);
        parentNode.removeChild(this);
    }
    
    /**
     * Replace this node in the DOM with the supplied node.
     * @param in the node that will will replace the existing node.
     */
    public void replaceWith(Node in) {
        Validate.notNull(in);
        Validate.notNull(parentNode);
        parentNode.replaceChild(this, in);
    }

    protected void setParentNode(Node parentNode) {
        if (this.parentNode != null)
            this.parentNode.removeChild(this);
        this.parentNode = parentNode;
    }

    protected void replaceChild(Node out, Node in) {
        Validate.isTrue(out.parentNode == this);
        Validate.notNull(in);
        if (in.parentNode != null)
            in.parentNode.removeChild(in);
        
        Integer index = out.siblingIndex();
        childNodes.set(index, in);
        in.parentNode = this;
        in.setSiblingIndex(index);
        out.parentNode = null;
    }

    protected void removeChild(Node out) {
        Validate.isTrue(out.parentNode == this);
        int index = out.siblingIndex();
        childNodes.remove(index);
        reindexChildren();
        out.parentNode = null;
    }

    protected void addChildren(Node... children) {
        //most used. short circuit addChildren(int), which hits reindex children and array copy
        for (Node child: children) {
            reparentChild(child);
            childNodes.add(child);
            child.setSiblingIndex(childNodes.size()-1);
        }
    }

    protected void addChildren(int index, Node... children) {
        Validate.noNullElements(children);
        for (int i = children.length - 1; i >= 0; i--) {
            Node in = children[i];
            reparentChild(in);
            childNodes.add(index, in);
        }
        reindexChildren();
    }

    private void reparentChild(Node child) {
        if (child.parentNode != null)
            child.parentNode.removeChild(child);
        child.setParentNode(this);
    }
    
    private void reindexChildren() {
        for (int i = 0; i < childNodes.size(); i++) {
            childNodes.get(i).setSiblingIndex(i);
        }
    }
    
    /**
     Retrieves this node's sibling nodes. Effectively, {@link #childNodes()  node.parent.childNodes()}.
     @return node siblings, including this node
     */
    public List<Node> siblingNodes() {
        return parent().childNodes(); // TODO: should this strip out this node? i.e. not a sibling of self?
    }

    /**
     Get this node's next sibling.
     @return next sibling, or null if this is the last sibling
     */
    public Node nextSibling() {
        if (parentNode == null)
            return null; // root
        
        List<Node> siblings = parentNode.childNodes;
        Integer index = siblingIndex();
        Validate.notNull(index);
        if (siblings.size() > index+1)
            return siblings.get(index+1);
        else
            return null;
    }

    /**
     Get this node's previous sibling.
     @return the previous sibling, or null if this is the first sibling
     */
    public Node previousSibling() {
        List<Node> siblings = parentNode.childNodes;
        Integer index = siblingIndex();
        Validate.notNull(index);
        if (index > 0)
            return siblings.get(index-1);
        else
            return null;
    }

    /**
     * Get the list index of this node in its node sibling list. I.e. if this is the first node
     * sibling, returns 0.
     * @return position in node sibling list
     * @see org.jsoup.nodes.Element#elementSiblingIndex()
     */
    public Integer siblingIndex() {
        return siblingIndex;
    }
    
    protected void setSiblingIndex(int siblingIndex) {
        this.siblingIndex = siblingIndex;
    }

    /**
     Get the outer HTML of this node.
     @return HTML
     */
    public String outerHtml() {
        StringBuilder accum = new StringBuilder(32*1024);
        outerHtml(accum);
        return accum.toString();
    }

    protected void outerHtml(StringBuilder accum) {
        new NodeTraversor(new OuterHtmlVisitor(accum, ownerDocument().outputSettings())).traverse(this);
    }

    /**
     Get the outer HTML of this node.
     @param accum accumulator to place HTML into
     */
    abstract void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out);

    abstract void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out);

    public String toString() {
        return outerHtml();
    }

    protected void indent(StringBuilder accum, int depth, Document.OutputSettings out) {
        accum.append("\n").append(StringUtil.padding(depth * out.indentAmount()));
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

    private static class OuterHtmlVisitor implements NodeVisitor {
        private StringBuilder accum;
        private Document.OutputSettings out;

        OuterHtmlVisitor(StringBuilder accum, Document.OutputSettings out) {
            this.accum = accum;
            this.out = out;
        }

        public void head(Node node, int depth) {
            node.outerHtmlHead(accum, depth, out);
        }

        public void tail(Node node, int depth) {
            if (!node.nodeName().equals("#text")) // saves a void hit.
                node.outerHtmlTail(accum, depth, out);
        }
    }
}
