package org.jsoup.nodes;

import org.jsoup.helper.Validate;

import java.util.List;

abstract class LeafNode extends Node {
    Object value; // either a string value, or an attribute map (in the rare case multiple attributes are set)

    protected final boolean hasAttributes() {
        return value instanceof Attributes;
    }

    @Override
    public final Attributes attributes() {
        ensureAttributes();
        return (Attributes) value;
    }

    private void ensureAttributes() {
        if (!hasAttributes()) {
            Object coreValue = value;
            Attributes attributes = new Attributes();
            value = attributes;
            if (coreValue != null)
                attributes.put(nodeName(), (String) coreValue);
        }
    }

    String coreValue() {
        return attr(nodeName());
    }

    void coreValue(String value) {
        attr(nodeName(), value);
    }

    @Override
    public String attr(String key) {
        Validate.notNull(key);
        if (!hasAttributes()) {
            return key.equals(nodeName()) ? (String) value : EmptyString;
        }
        return super.attr(key);
    }

    @Override
    public Node attr(String key, String value) {
        if (!hasAttributes() && key.equals(nodeName())) {
            this.value = value;
        } else {
            ensureAttributes();
            super.attr(key, value);
        }
        return this;
    }

    @Override
    public boolean hasAttr(String key) {
        ensureAttributes();
        return super.hasAttr(key);
    }

    @Override
    public Node removeAttr(String key) {
        ensureAttributes();
        return super.removeAttr(key);
    }

    @Override
    public String absUrl(String key) {
        ensureAttributes();
        return super.absUrl(key);
    }

    @Override
    public String baseUri() {
        return hasParent() ? parent().baseUri() : "";
    }

    @Override
    protected void doSetBaseUri(String baseUri) {
        // noop
    }

    @Override
    public int childNodeSize() {
        return 0;
    }

    @Override
    protected List<Node> ensureChildNodes() {
        throw new UnsupportedOperationException("Leaf Nodes do not have child nodes.");
    }
}
