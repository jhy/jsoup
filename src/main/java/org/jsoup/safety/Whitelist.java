package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

/**
 @deprecated As of release <code>v1.14.1</code>, this class is deprecated in favour of {@link Safelist}. The name has
 been changed with the intent of promoting more inclusive language. {@link Safelist} is a drop-in replacement, and no
 further changes other than updating the name in your code are required to cleanly migrate. This class will be
 removed in <code>v1.15.1</code>. Until that release, this class acts as a shim to maintain code compatibility
 (source and binary).
 <p>
 For a clear rationale of the removal of this change, please see
 <a href="https://tools.ietf.org/html/draft-knodel-terminology-04" title="draft-knodel-terminology-04">Terminology,
 Power, and Inclusive Language in Internet-Drafts and RFCs</a> */
@Deprecated
public class Whitelist extends Safelist {
    public Whitelist() {
        super();
    }

    public Whitelist(Safelist copy) {
        super(copy);
    }

    static public Whitelist basic() {
        return new Whitelist(Safelist.basic());
    }

    static public Whitelist basicWithImages() {
        return new Whitelist(Safelist.basicWithImages());
    }

    static public Whitelist none() {
        return new Whitelist(Safelist.none());
    }

    static public Whitelist relaxed() {
        return new Whitelist(Safelist.relaxed());
    }

    static public Whitelist simpleText() {
        return new Whitelist(Safelist.simpleText());
    }

    @Override
    public Whitelist addTags(String... tags) {
        super.addTags(tags);
        return this;
    }

    @Override
    public Whitelist removeTags(String... tags) {
        super.removeTags(tags);
        return this;
    }

    @Override
    public Whitelist addAttributes(String tag, String... attributes) {
        super.addAttributes(tag, attributes);
        return this;
    }

    @Override
    public Whitelist removeAttributes(String tag, String... attributes) {
        super.removeAttributes(tag, attributes);
        return this;
    }

    @Override
    public Whitelist addEnforcedAttribute(String tag, String attribute, String value) {
        super.addEnforcedAttribute(tag, attribute, value);
        return this;
    }

    @Override
    public Whitelist removeEnforcedAttribute(String tag, String attribute) {
        super.removeEnforcedAttribute(tag, attribute);
        return this;
    }

    @Override
    public Whitelist preserveRelativeLinks(boolean preserve) {
        super.preserveRelativeLinks(preserve);
        return this;
    }

    @Override
    public Whitelist addProtocols(String tag, String attribute, String... protocols) {
        super.addProtocols(tag, attribute, protocols);
        return this;
    }

    @Override
    public Whitelist removeProtocols(String tag, String attribute, String... removeProtocols) {
        super.removeProtocols(tag, attribute, removeProtocols);
        return this;
    }

    @Override
    protected boolean isSafeTag(String tag) {
        return super.isSafeTag(tag);
    }

    @Override
    protected boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
        return super.isSafeAttribute(tagName, el, attr);
    }

    @Override
    Attributes getEnforcedAttributes(String tagName) {
        return super.getEnforcedAttributes(tagName);
    }
}
