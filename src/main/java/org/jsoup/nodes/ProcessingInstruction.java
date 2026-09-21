package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.internal.QuietAppendable;
import org.jsoup.internal.StringUtil;
import org.jsoup.parser.Parser;

/**
 A processing instruction with a target and data, such as {@code <?marker name="country-options"?>}.

 <p>The XML declaration {@code <?xml ...?>} is represented by {@link XmlDeclaration}.</p>
 <p>If you access this node's attributes, directly or through an attribute selector, the instruction data is parsed into an {@link Attributes} object using the owning document's parser settings, or HTML settings if the node is detached.
 The attributes then determine the instruction data.</p>

 @since 1.24.1 */
public class ProcessingInstruction extends LeafNode {
    private final String target;
    private int dataStartPos = -1; // source offset used to rebase parsed attribute ranges; -1 if untracked

    /**
     Creates a processing instruction with the supplied target and data.
     @param target the target following {@code <?}
     @param data the data following the target, without the separating whitespace or closing {@code ?>}
     */
    public ProcessingInstruction(String target, String data) {
        super(data);
        Validate.notNull(target);
        this.target = target;
    }

    /** Returns the node name {@code #processing-instruction}. */
    @Override public String nodeName() {
        return "#processing-instruction";
    }

    /**
     Returns the instruction target.
     @return the target following {@code <?}
     */
    public String target() {
        return target;
    }

    /**
     Returns the data following the target.
     If the attributes have been accessed, returns their serialized form.
     @return the instruction data
     */
    public String data() {
        if (!hasAttributes()) return coreValue();

        Document owner = ownerDocument();
        Document.OutputSettings out = owner != null ? owner.outputSettings() : new Document.OutputSettings();
        return data(out);
    }

    /** Serializes the parsed instruction attributes. */
    private String data(Document.OutputSettings out) {
        StringBuilder data = StringUtil.borrowBuilder();
        attributes().html(QuietAppendable.wrap(data), out);
        if (data.length() > 0)
            data.deleteCharAt(0); // drop the separator Attributes writes before each attribute
        return StringUtil.releaseBuilder(data);
    }

    /**
     Replaces the instruction data.
     If the attributes have been accessed, parses the new data and updates the existing {@link Attributes} object.
     @param data the new data, without the separating whitespace or closing {@code ?>}
     @return this node, for chaining
     */
    public ProcessingInstruction data(String data) {
        Validate.notNull(data);
        dataStartPos = -1;
        if (hasAttributes()) {
            Attributes parsed = parseAttributes(data);
            super.clearAttributes();
            attributes().addAll(parsed);
        } else {
            coreValue(data);
        }
        return this;
    }

    /** Returns the instruction data as this node's value. */
    @Override public String nodeValue() {
        return data();
    }

    /** Parses the instruction data into attribute storage when requested. */
    @Override Attributes newAttributes(String data) {
        return parseAttributes(data);
    }

    /** Parses the instruction data without changing the owning document's parser state. */
    private Attributes parseAttributes(String data) {
        Document owner = ownerDocument();
        Parser parser = owner != null ? owner.parser().newInstance() : Parser.htmlParser();
        Range sourceRange = sourceRange();
        boolean trackPosition = dataStartPos >= 0 && sourceRange.isTracked();
        parser.setTrackPosition(trackPosition);
        Attributes attributes = parser.parseAttributes(data);
        if (trackPosition) {
            Range.Spans spans = attributes.spans();
            if (spans != null)
                spans.rebaseAttributeRanges(sourceRange, dataStartPos);
        }
        return attributes;
    }

    void sourceDataStart(int pos) {
        dataStartPos = pos;
    }

    @Override public String attr(String key) {
        ensureAttributes();
        return super.attr(key);
    }

    @Override public ProcessingInstruction attr(String key, String value) {
        ensureAttributes();
        super.attr(key, value);
        return this;
    }

    @Override public int attributesSize() {
        return attributes().size();
    }

    @Override public ProcessingInstruction clearAttributes() {
        ensureAttributes();
        super.clearAttributes();
        return this;
    }

    @Override void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        String data = hasAttributes() ? data(out) : coreValue();
        accum.append("<?").append(target);
        if (out.syntax() == Document.OutputSettings.Syntax.html || !data.isEmpty())
            accum.append(' ');
        accum.append(data).append("?>");
    }

    @Override public ProcessingInstruction clone() {
        return (ProcessingInstruction) super.clone();
    }
}
