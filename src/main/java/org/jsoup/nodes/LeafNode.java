package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.internal.QuietAppendable;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 A node that does not hold any children. E.g.: {@link TextNode}, {@link DataNode}, {@link Comment}.
 */
public abstract class LeafNode extends Node {
    Object value; // either a string, tracked string, or attributes object

    public LeafNode() {
        value = "";
    }

    protected LeafNode(String coreValue) {
        Validate.notNull(coreValue);
        value = coreValue;
    }

    @Override protected final boolean hasAttributes() {
        return value instanceof Attributes;
    }

    @Override
    public final Attributes attributes() {
        ensureAttributes();
        return (Attributes) value;
    }

    private void ensureAttributes() {
        if (!hasAttributes()) {
            String coreValue = coreValue();
            Attributes attributes = new Attributes();
            Range.Spans rangeSpans = spans();
            value = attributes;
            attributes.put(nodeName(), coreValue);
            if (rangeSpans != null)
                attributes.putSpans(rangeSpans);
        }
    }

    String coreValue() {
        if (value instanceof Attributes)   return ((Attributes) value).get(nodeName());
        if (value instanceof TrackedValue) return ((TrackedValue) value).coreValue;
        return (String) value;
    }

    @Override @Nullable
    public Element parent() {
        return parentNode;
    }

    @Override
    public String nodeValue() {
        return coreValue();
    }

    void coreValue(String value) {
        if (this.value instanceof Attributes)
            ((Attributes) this.value).put(nodeName(), value);
        else if (this.value instanceof TrackedValue)
            ((TrackedValue) this.value).coreValue = value;
        else
            this.value = value;
    }

    @Override
    public String attr(String key) {
        if (!hasAttributes())
            return nodeName().equals(key) ? coreValue() : EmptyString;
        return super.attr(key);
    }

    @Override
    public Node attr(String key, String value) {
        if (!hasAttributes() && key.equals(nodeName())) {
            coreValue(value);
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
        return parentNode != null ? parentNode.baseUri() : "";
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
    public Node empty() {
        return this;
    }

    @Override
    protected List<Node> ensureChildNodes() {
        return EmptyNodes;
    }

    @Override
    void outerHtmlTail(QuietAppendable accum, Document.OutputSettings out) {}

    @Override
    protected LeafNode doClone(Node parent) {
        LeafNode clone = (LeafNode) super.doClone(parent);

        // Object value could be plain string, tracked string, or attributes - need to clone.
        if (hasAttributes())
            clone.value = ((Attributes) value).clone();
        else if (value instanceof TrackedValue)
            clone.value = ((TrackedValue) value).copy();

        return clone;
    }

    @Override Range.@Nullable Spans spans() {
        if (value instanceof TrackedValue)
            return ((TrackedValue) value).spans;
        return super.spans();
    }

    @Override Range.Spans ensureSpans() {
        // Leaf nodes normally hold just their core string. When source ranges are tracked, keep the string plus spans
        // in a small wrapper so leaf nodes do not expand to Attributes just for parser metadata. If attributes are
        // later requested, ensureAttributes() moves these same spans into the Attributes object.
        if (value instanceof TrackedValue)
            return ((TrackedValue) value).spans;
        if (value instanceof Attributes)
            return ((Attributes) value).ensureSpans();

        TrackedValue trackedValue = new TrackedValue((String) value);
        value = trackedValue;
        return trackedValue.spans;
    }

    /**
     Holds a compact leaf value plus ranges without expanding to Attributes.
     */
    private static final class TrackedValue {
        String coreValue;
        final Range.Spans spans;

        /**
         Creates a tracked leaf value around the core text.
         */
        TrackedValue(String coreValue) {
            this(coreValue, new Range.Spans());
        }

        /**
         Creates a tracked leaf value around copied range spans.
         */
        TrackedValue(String coreValue, Range.Spans spans) {
            this.coreValue = coreValue;
            this.spans = spans;
        }

        /**
         Returns a copy so cloned leaf nodes can mutate range spans independently.
         */
        TrackedValue copy() {
            return new TrackedValue(coreValue, spans.copy());
        }
    }
}
