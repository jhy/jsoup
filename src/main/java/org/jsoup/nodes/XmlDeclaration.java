package org.jsoup.nodes;

import org.jsoup.internal.QuietAppendable;
import org.jsoup.internal.StringUtil;


/**
 * An XML declaration, such as {@code <?xml version="1.0"?>}, or a markup declaration, such as {@code <!ELEMENT ...>}.
 * Use {@link ProcessingInstruction} for other {@code <?target data?>} nodes.
 * Declaration values are available through {@link #attributes()} and {@link #attr(String)}.
 */
public class XmlDeclaration extends LeafNode {

    /** Whether this is a markup declaration. */
    private final boolean isMarkupDeclaration;

    /**
     * Creates an XML declaration or markup declaration.
     * @param name the declaration name, such as {@code xml} or {@code ELEMENT}
     * @param isMarkupDeclaration {@code true} to create {@code <!name ...>}; {@code false} to create {@code <?name ...?>}
     */
    public XmlDeclaration(String name, boolean isMarkupDeclaration) {
        super(name);
        this.isMarkupDeclaration = isMarkupDeclaration;
    }

    @Override public String nodeName() {
        return "#declaration";
    }

    /**
     * Gets the declaration name.
     * @return the declaration name
     */
    public String name() {
        return coreValue();
    }

    /**
     * Gets the declaration data, without its name or delimiters.
     * @return the declaration data
     */
    public String getWholeDeclaration() {
        StringBuilder sb = StringUtil.borrowBuilder();
        getWholeDeclaration(QuietAppendable.wrap(sb), new Document.OutputSettings());
        return StringUtil.releaseBuilder(sb).trim();
    }

    private void getWholeDeclaration(QuietAppendable accum, Document.OutputSettings out) {
        for (Attribute attribute : attributes()) {
            String key = attribute.getKey();
            String val = attribute.getValue();
            if (!key.equals(nodeName())) { // skips coreValue (name)
                accum.append(' ');
                // basically like Attribute, but skip empty vals in XML
                accum.append(key);
                if (!val.isEmpty()) {
                    accum.append("=\"");
                    Entities.escape(accum, val, out, Entities.ForAttribute);
                    accum.append('"');
                }
            }
        }
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        accum
            .append("<")
            .append(isMarkupDeclaration ? "!" : "?")
            .append(coreValue());
        getWholeDeclaration(accum, out);
        accum
            .append(isMarkupDeclaration ? "" : "?")
            .append(">");
    }

    @Override
    void outerHtmlTail(QuietAppendable accum, Document.OutputSettings out) {
    }

    @Override
    public String toString() {
        return outerHtml();
    }

    @Override
    public XmlDeclaration clone() {
        return (XmlDeclaration) super.clone();
    }
}
