package org.jsoup.parser;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Attributes;

/**
 A start tag consists of a Tag and Attributes.

 @author Jonathan Hedley, jonathan@hedley.net */
public class StartTag {
    Tag tag;
    Attributes attributes;

    public StartTag(Tag tag, Attributes attributes) {
        Validate.notNull(attributes);
        this.tag = tag;
        this.attributes = attributes;
    }

    public StartTag(Tag tag) {
        this(tag, new Attributes());
    }

    public Tag getTag() {
        return tag;
    }

    public Attributes getAttributes() {
        return attributes;
    }
}
