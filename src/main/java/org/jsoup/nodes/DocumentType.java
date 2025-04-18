package org.jsoup.nodes;

import org.jsoup.internal.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * A {@code <!DOCTYPE>} node.
 */
public class DocumentType extends LeafNode {
    // todo needs a bit of a chunky cleanup. this level of detail isn't needed
    public static final String PUBLIC_KEY = "PUBLIC";
    public static final String SYSTEM_KEY = "SYSTEM";
    private static final String NameKey = "name";
    private static final String PubSysKey = "pubSysKey"; // PUBLIC or SYSTEM
    private static final String PublicId = "publicId";
    private static final String SystemId = "systemId";
    // todo: quirk mode from publicId and systemId

    /**
     * Create a new doctype element.
     * @param name the doctype's name
     * @param publicId the doctype's public ID
     * @param systemId the doctype's system ID
     */
    public DocumentType(String name, String publicId, String systemId) {
        super(name);
        Validate.notNull(publicId);
        Validate.notNull(systemId);
        attributes()
            .add(NameKey, name)
            .add(PublicId, publicId)
            .add(SystemId, systemId);
        updatePubSyskey();
    }

    public void setPubSysKey(@Nullable String value) {
        if (value != null)
            attr(PubSysKey, value);
    }

    private void updatePubSyskey() {
        if (has(PublicId)) {
            attributes().add(PubSysKey, PUBLIC_KEY);
        } else if (has(SystemId))
            attributes().add(PubSysKey, SYSTEM_KEY);
    }

    /**
     * Get this doctype's name (when set, or empty string)
     * @return doctype name
     */
    public String name() {
        return attr(NameKey);
    }

    /**
     * Get this doctype's Public ID (when set, or empty string)
     * @return doctype Public ID
     */
    public String publicId() {
        return attr(PublicId);
    }

    /**
     * Get this doctype's System ID (when set, or empty string)
     * @return doctype System ID
     */
    public String systemId() {
        return attr(SystemId);
    }

    @Override
    public String nodeName() {
        return "#doctype";
    }

    @Override
    void outerHtmlHead(Appendable accum, Document.OutputSettings out) throws IOException {
        if (out.syntax() == Syntax.html && !has(PublicId) && !has(SystemId)) {
            // looks like a html5 doctype, go lowercase for aesthetics
            accum.append("<!doctype");
        } else {
            accum.append("<!DOCTYPE");
        }
        if (has(NameKey))
            accum.append(" ").append(attr(NameKey));
        if (has(PubSysKey))
            accum.append(" ").append(attr(PubSysKey));
        if (has(PublicId))
            accum.append(" \"").append(attr(PublicId)).append('"');
        if (has(SystemId))
            accum.append(" \"").append(attr(SystemId)).append('"');
        accum.append('>');
    }


    private boolean has(final String attribute) {
        return !StringUtil.isBlank(attr(attribute));
    }
}
