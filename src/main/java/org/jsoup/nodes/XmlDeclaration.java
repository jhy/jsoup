package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.internal.StringUtil;

import java.io.IOException;

/**
 * An XML Declaration. Includes support for treating the declaration contents as pseudo attributes.
 */
public class XmlDeclaration extends LeafNode {

    /**
     First char is `!` if isDeclaration, like in {@code  <!ENTITY ...>}.
     Otherwise, is `?`, a processing instruction, like {@code <?xml .... ?>} (and note trailing `?`).
     */
    private final boolean isDeclaration;

    /**
     * Create a new XML declaration
     * @param name of declaration
     * @param isDeclaration {@code true} if a declaration (first char is `!`), otherwise a processing instruction (first char is `?`).
     */
    public XmlDeclaration(String name, boolean isDeclaration) {
        super(name);
        this.isDeclaration = isDeclaration;
    }

    @Override public String nodeName() {
        return "#declaration";
    }

    /**
     * Get the name of this declaration.
     * @return name of this declaration.
     */
    public String name() {
        return coreValue();
    }

    /**
     * Get the unencoded XML declaration.
     * @return XML declaration
     */
    public String getWholeDeclaration() {
        StringBuilder sb = StringUtil.borrowBuilder();
        try {
            getWholeDeclaration(sb, new Document.OutputSettings());
        } catch (IOException e) {
            throw new SerializationException(e);
        }
        return StringUtil.releaseBuilder(sb).trim();
    }

    private void getWholeDeclaration(Appendable accum, Document.OutputSettings out) throws IOException {
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
    void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        accum
            .append("<")
            .append(isDeclaration ? "!" : "?")
            .append(coreValue());
        getWholeDeclaration(accum, out);
        accum
            .append(isDeclaration ? "" : "?")
            .append(">");
    }

    @Override
    void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) {
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
