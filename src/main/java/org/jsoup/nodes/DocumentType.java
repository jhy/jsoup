package org.jsoup.nodes;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document.OutputSettings.*;

/**
 * A {@code <!DOCTYPE>} node.
 */
public class DocumentType extends Node {
    private static final String NAME = "name";
    private static final String PUBLIC_ID = "publicId";
    private static final String SYSTEM_ID = "systemId";
    // todo: quirk mode from publicId and systemId

    /**
     * Create a new doctype element.
     * @param name the doctype's name
     * @param publicId the doctype's public ID
     * @param systemId the doctype's system ID
     * @param baseUri the doctype's base URI
     */
    public DocumentType(String name, String publicId, String systemId, String baseUri) {
        super(baseUri);

        attr(NAME, name);
        attr(PUBLIC_ID, publicId);
        attr(SYSTEM_ID, systemId);
    }

    @Override
    public String nodeName() {
        return "#doctype";
    }

    @Override
    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (out.syntax() == Syntax.html && !has(PUBLIC_ID) && !has(SYSTEM_ID)) {
            // looks like a html5 doctype, go lowercase for aesthetics
            accum.append("<!doctype");
        } else {
            accum.append("<!DOCTYPE");
        }
        if (has(NAME))
            accum.append(" ").append(attr(NAME));
        if (has(PUBLIC_ID))
            accum.append(" PUBLIC \"").append(attr(PUBLIC_ID)).append('"');
        if (has(SYSTEM_ID))
            accum.append(" \"").append(attr(SYSTEM_ID)).append('"');
        accum.append('>');
    }

    @Override
    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
    }

    private boolean has(final String attribute) {
        return !StringUtil.isBlank(attr(attribute));
    }
}
