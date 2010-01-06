package org.jsoup.parser;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Attributes;

/**
 A start tag consists of a Tag and Attributes.

 @author Jonathan Hedley, jonathan@hedley.net */
public class StartTag {
    final Tag tag;
    final Attributes attributes;
    final String baseUri;

    public StartTag(Tag tag, String baseUri, Attributes attributes) {
        Validate.notNull(attributes);
        Validate.notNull(baseUri);

        this.tag = tag;
        this.baseUri = baseUri;
        this.attributes = attributes;
    }

    public StartTag(Tag tag, String baseUri) {
        this(tag, baseUri, new Attributes());
    }

    public Tag getTag() {
        return tag;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public String getBaseUri() {
        return baseUri;
    }
}
