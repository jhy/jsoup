package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist.TagName;

public class Whitelist {

    protected Safelist safelist;

    public Whitelist() {
        this.safelist = new Safelist();
    }

    public Whitelist(Whitelist copy) {
        this.safelist = new Safelist(copy.safelist);
    }

    private Whitelist(Safelist embedded) {
        this.safelist = embedded;
    }

    public static Whitelist none() {
        return new Whitelist();
    }

    public static Whitelist simpleText() {
        return new Whitelist(Safelist.simpleText());
    }

    public static Whitelist basic() {
        return new Whitelist(Safelist.basic());
    }

    public Safelist getSafelist() {
        return this.safelist;
    }

    public Whitelist addTags(String... tags) {
        this.safelist.addTags(tags);
        return this;
    }

    public Whitelist removeTags(String... tags) {
        this.safelist.removeTags(tags);
        return this;
    }

    public Whitelist addAttributes(String tag, String... attributes) {
        this.safelist.addAttributes(tag, attributes);
        return this;
    }

    public Whitelist removeAttributes(String tag, String... attributes) {
        this.safelist.removeAttributes(tag, attributes);
        return this;
    }

    public Whitelist addEnforcedAttribute(String tag, String attribute, String value) {
        this.safelist.addEnforcedAttribute(tag, attribute, value);
        return this;
    }

    public Whitelist removeEnforcedAttribute(String tag, String attribute) {
        this.safelist.removeEnforcedAttribute(tag, attribute);
        return this;
    }

    public Whitelist preserveRelativeLinks(boolean preserve) {
        this.safelist.setPreserverRelativeLinks(preserve);
        return this;
    }

    public Whitelist addProtocols(String tag, String attribute, String... protocols) {
        this.safelist.addProtocols(tag, attribute, protocols);
        return this;
    }

    public Whitelist removeProtocols(String tag, String attribute, String... removeProtocols) {
        this.safelist.removeProtocols(tag, attribute, removeProtocols);
        return this;
    }

    public boolean isSafeTag(String tag) {
        return this.safelist.getTagNames().contains(TagName.valueOf(tag));
    }

    public boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
        return this.safelist.isSafeAttribute(tagName, el, attr);
    }

    public Attributes getEnforcedAttributes(String tagName) {
        return this.safelist.getEnforcedAttributes(tagName);
    }
}
