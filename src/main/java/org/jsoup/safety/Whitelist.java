package org.jsoup.safety;

/**
 * This class is deprecated and will be removed in version 2 of jsoup.
 * Please change your code to use the class {@link Allowlist} instead.
 */
@Deprecated
public class Whitelist extends Allowlist {

    private final Allowlist allowlist;

    private Whitelist mirror(Allowlist allowlist) {
        tagNames = allowlist.tagNames;
        attributes = allowlist.attributes;
        enforcedAttributes = allowlist.enforcedAttributes;
        preserveRelativeLinks = allowlist.preserveRelativeLinks;
        return this;
    }

    public static Whitelist none() {
        return new Whitelist(Allowlist.none());
    }

    public static Whitelist simpleText() {
        return new Whitelist(Allowlist.simpleText());
    }

    public static Whitelist basic() {
        return new Whitelist(Allowlist.basic());
    }

    public static Whitelist basicWithImages() {
        return new Whitelist(Allowlist.basicWithImages());
    }

    public static Whitelist relaxed() {
        return new Whitelist(Allowlist.relaxed());
    }

    public Whitelist() {
        this(new Allowlist());
    }

    private Whitelist(Allowlist allowlist) {
        this.allowlist = allowlist;
        mirror(allowlist);
    }

    @Override
    public Whitelist addTags(String... tags) {
        return mirror(allowlist.addTags(tags));
    }

    @Override
    public Whitelist removeTags(String... tags) {
        return mirror(allowlist.removeTags(tags));
    }

    @Override
    public Whitelist addAttributes(String tag, String... attributes) {
        return mirror(allowlist.addAttributes(tag, attributes));
    }

    @Override
    public Whitelist removeAttributes(String tag, String... attributes) {
        return mirror(allowlist.removeAttributes(tag, attributes));
    }

    @Override
    public Whitelist addEnforcedAttribute(String tag, String attribute, String value) {
        return mirror(allowlist.addEnforcedAttribute(tag, attribute, value));
    }

    @Override
    public Whitelist removeEnforcedAttribute(String tag, String attribute) {
        return mirror(allowlist.removeEnforcedAttribute(tag, attribute));
    }

    @Override
    public Whitelist preserveRelativeLinks(boolean preserve) {
        return mirror(allowlist.preserveRelativeLinks(preserve));
    }

    @Override
    public Whitelist addProtocols(String tag, String attribute, String... protocols) {
        return mirror(allowlist.addProtocols(tag, attribute, protocols));
    }

    @Override
    public Whitelist removeProtocols(String tag, String attribute, String... removeProtocols) {
        return mirror(allowlist.removeProtocols(tag, attribute, removeProtocols));
    }

    public Allowlist asAllowlist() {
        return allowlist;
    }
}

