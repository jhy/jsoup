package org.jsoup.parser;

import org.jsoup.helper.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTML Tag capabilities.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class Tag {
    private static final Map<String, Tag> tags = new HashMap<String, Tag>(); // map of known tags

    private String tagName;
    private boolean isBlock = true; // block or inline
    private boolean formatAsBlock = true; // should be formatted as a block
    private boolean canContainBlock = true; // Can this tag hold block level tags?
    private boolean canContainInline = true; // only pcdata if not
    private boolean empty = false; // can hold nothing; e.g. img
    private boolean selfClosing = false; // can self close (<foo />). used for unknown tags that self close, without forcing them as empty.
    private boolean preserveWhitespace = false; // for pre, textarea, script etc

    private Tag(String tagName) {
        this.tagName = tagName.toLowerCase();
    }

    /**
     * Get this tag's name.
     *
     * @return the tag's name
     */
    public String getName() {
        return tagName;
    }

    /**
     * Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
     * <p/>
     * Pre-defined tags (P, DIV etc) will be ==, but unknown tags are not registered and will only .equals().
     *
     * @param tagName Name of tag, e.g. "p". Case insensitive.
     * @return The tag, either defined or new generic.
     */
    public static Tag valueOf(String tagName) {
        Validate.notNull(tagName);
        Tag tag = tags.get(tagName);

        if (tag == null) {
            tagName = tagName.trim().toLowerCase();
            Validate.notEmpty(tagName);
            tag = tags.get(tagName);

            if (tag == null) {
                // not defined: create default; go anywhere, do anything! (incl be inside a <p>)
                tag = new Tag(tagName);
                tag.isBlock = false;
                tag.canContainBlock = true;
            }
        }
        return tag;
    }

    /**
     * Gets if this is a block tag.
     *
     * @return if block tag
     */
    public boolean isBlock() {
        return isBlock;
    }

    /**
     * Gets if this tag should be formatted as a block (or as inline)
     *
     * @return if should be formatted as block or inline
     */
    public boolean formatAsBlock() {
        return formatAsBlock;
    }

    /**
     * Gets if this tag can contain block tags.
     *
     * @return if tag can contain block tags
     */
    public boolean canContainBlock() {
        return canContainBlock;
    }

    /**
     * Gets if this tag is an inline tag.
     *
     * @return if this tag is an inline tag.
     */
    public boolean isInline() {
        return !isBlock;
    }

    /**
     * Gets if this tag is a data only tag.
     *
     * @return if this tag is a data only tag
     */
    public boolean isData() {
        return !canContainInline && !isEmpty();
    }

    /**
     * Get if this is an empty tag
     *
     * @return if this is an empty tag
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Get if this tag is self closing.
     *
     * @return if this tag should be output as self closing.
     */
    public boolean isSelfClosing() {
        return empty || selfClosing;
    }

    /**
     * Get if this is a pre-defined tag, or was auto created on parsing.
     *
     * @return if a known tag
     */
    public boolean isKnownTag() {
        return tags.containsKey(tagName);
    }

    /**
     * Check if this tagname is a known tag.
     *
     * @param tagName name of tag
     * @return if known HTML tag
     */
    public static boolean isKnownTag(String tagName) {
        return tags.containsKey(tagName);
    }

    /**
     * Get if this tag should preserve whitespace within child text nodes.
     *
     * @return if preserve whitepace
     */
    public boolean preserveWhitespace() {
        return preserveWhitespace;
    }

    Tag setSelfClosing() {
        selfClosing = true;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;

        Tag tag = (Tag) o;

        if (canContainBlock != tag.canContainBlock) return false;
        if (canContainInline != tag.canContainInline) return false;
        if (empty != tag.empty) return false;
        if (formatAsBlock != tag.formatAsBlock) return false;
        if (isBlock != tag.isBlock) return false;
        if (preserveWhitespace != tag.preserveWhitespace) return false;
        if (selfClosing != tag.selfClosing) return false;
        if (!tagName.equals(tag.tagName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = tagName.hashCode();
        result = 31 * result + (isBlock ? 1 : 0);
        result = 31 * result + (formatAsBlock ? 1 : 0);
        result = 31 * result + (canContainBlock ? 1 : 0);
        result = 31 * result + (canContainInline ? 1 : 0);
        result = 31 * result + (empty ? 1 : 0);
        result = 31 * result + (selfClosing ? 1 : 0);
        result = 31 * result + (preserveWhitespace ? 1 : 0);
        return result;
    }

    public String toString() {
        return tagName;
    }

    // internal static initialisers:
    // prepped from http://www.w3.org/TR/REC-html40/sgml/dtd.html and other sources
    private static final String[] blockTags = {
            "html", "head", "body", "frameset", "script", "noscript", "style", "meta", "link", "title", "frame",
            "noframes", "section", "nav", "aside", "hgroup", "header", "footer", "p", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "pre", "div", "blockquote", "hr", "address", "figure", "figcaption", "form", "fieldset", "ins",
            "del", "s", "dl", "dt", "dd", "li", "table", "caption", "thead", "tfoot", "tbody", "colgroup", "col", "tr", "th",
            "td", "video", "audio", "canvas", "details", "menu", "plaintext"
    };
    private static final String[] inlineTags = {
            "object", "base", "font", "tt", "i", "b", "u", "big", "small", "em", "strong", "dfn", "code", "samp", "kbd",
            "var", "cite", "abbr", "time", "acronym", "mark", "ruby", "rt", "rp", "a", "img", "br", "wbr", "map", "q",
            "sub", "sup", "bdo", "iframe", "embed", "span", "input", "select", "textarea", "label", "button", "optgroup",
            "option", "legend", "datalist", "keygen", "output", "progress", "meter", "area", "param", "source", "track",
            "summary", "command", "device"
    };
    private static final String[] emptyTags = {
            "meta", "link", "base", "frame", "img", "br", "wbr", "embed", "hr", "input", "keygen", "col", "command",
            "device"
    };
    private static final String[] formatAsInlineTags = {
            "title", "a", "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "li", "th", "td", "script", "style",
            "ins", "del", "s"
    };
    private static final String[] preserveWhitespaceTags = {"pre", "plaintext", "title", "textarea"};

    static {
        // creates
        for (String tagName : blockTags) {
            Tag tag = new Tag(tagName);
            register(tag);
        }
        for (String tagName : inlineTags) {
            Tag tag = new Tag(tagName);
            tag.isBlock = false;
            tag.canContainBlock = false;
            tag.formatAsBlock = false;
            register(tag);
        }

        // mods:
        for (String tagName : emptyTags) {
            Tag tag = tags.get(tagName);
            Validate.notNull(tag);
            tag.canContainBlock = false;
            tag.canContainInline = false;
            tag.empty = true;
        }

        for (String tagName : formatAsInlineTags) {
            Tag tag = tags.get(tagName);
            Validate.notNull(tag);
            tag.formatAsBlock = false;
        }

        for (String tagName : preserveWhitespaceTags) {
            Tag tag = tags.get(tagName);
            Validate.notNull(tag);
            tag.preserveWhitespace = true;
        }
    }

    private static void register(Tag tag) {
        tags.put(tag.tagName, tag);
    }
}
