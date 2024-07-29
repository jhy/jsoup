package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.Normalizer;
import org.jsoup.internal.SharedConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tag capabilities.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class Tag implements Cloneable {
    private static final Map<String, Tag> Tags = new HashMap<>(); // map of known tags

    private String tagName;
    private final String normalName; // always the lower case version of this tag, regardless of case preservation mode
    private String namespace;
    private boolean isBlock = true; // block
    private boolean formatAsBlock = true; // should be formatted as a block
    private boolean empty = false; // can hold nothing; e.g. img
    private boolean selfClosing = false; // can self close (<foo />). used for unknown tags that self close, without forcing them as empty.
    private boolean preserveWhitespace = false; // for pre, textarea, script etc
    private boolean formList = false; // a control that appears in forms: input, textarea, output etc
    private boolean formSubmit = false; // a control that can be submitted in a form: input etc

    private Tag(String tagName, String namespace) {
        this.tagName = tagName;
        normalName = Normalizer.lowerCase(tagName);
        this.namespace = namespace;
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
     * Get this tag's normalized (lowercased) name.
     * @return the tag's normal name.
     */
    public String normalName() {
        return normalName;
    }

    public String namespace() {
        return namespace;
    }

    /**
     * Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
     * <p>
     * Pre-defined tags (p, div etc) will be ==, but unknown tags are not registered and will only .equals().
     * </p>
     * 
     * @param tagName Name of tag, e.g. "p". Case-insensitive.
     * @param namespace the namespace for the tag.
     * @param settings used to control tag name sensitivity
     * @return The tag, either defined or new generic.
     */
    public static Tag valueOf(String tagName, String namespace, ParseSettings settings) {
        Validate.notEmpty(tagName);
        Validate.notNull(namespace);
        Tag tag = Tags.get(tagName);
        if (tag != null && tag.namespace.equals(namespace))
            return tag;

        tagName = settings.normalizeTag(tagName); // the name we'll use
        Validate.notEmpty(tagName);
        String normalName = Normalizer.lowerCase(tagName); // the lower-case name to get tag settings off
        tag = Tags.get(normalName);
        if (tag != null && tag.namespace.equals(namespace)) {
            if (settings.preserveTagCase() && !tagName.equals(normalName)) {
                tag = tag.clone(); // get a new version vs the static one, so name update doesn't reset all
                tag.tagName = tagName;
            }
            return tag;
        }

        // not defined: create default; go anywhere, do anything! (incl be inside a <p>)
        tag = new Tag(tagName, namespace);
        tag.isBlock = false;

        return tag;
    }

    /**
     * Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
     * <p>
     * Pre-defined tags (P, DIV etc) will be ==, but unknown tags are not registered and will only .equals().
     * </p>
     *
     * @param tagName Name of tag, e.g. "p". <b>Case sensitive</b>.
     * @return The tag, either defined or new generic.
     * @see #valueOf(String tagName, String namespace, ParseSettings settings)
     */
    public static Tag valueOf(String tagName) {
        return valueOf(tagName, Parser.NamespaceHtml, ParseSettings.preserveCase);
    }

    /**
     * Get a Tag by name. If not previously defined (unknown), returns a new generic tag, that can do anything.
     * <p>
     * Pre-defined tags (P, DIV etc) will be ==, but unknown tags are not registered and will only .equals().
     * </p>
     *
     * @param tagName Name of tag, e.g. "p". <b>Case sensitive</b>.
     * @param settings used to control tag name sensitivity
     * @return The tag, either defined or new generic.
     * @see #valueOf(String tagName, String namespace, ParseSettings settings)
     */
    public static Tag valueOf(String tagName, ParseSettings settings) {
        return valueOf(tagName, Parser.NamespaceHtml, settings);
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
     * Gets if this tag is an inline tag.
     *
     * @return if this tag is an inline tag.
     */
    public boolean isInline() {
        return !isBlock;
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
     * Get if this tag is self-closing.
     *
     * @return if this tag should be output as self-closing.
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
        return Tags.containsKey(tagName);
    }

    /**
     * Check if this tagname is a known tag.
     *
     * @param tagName name of tag
     * @return if known HTML tag
     */
    public static boolean isKnownTag(String tagName) {
        return Tags.containsKey(tagName);
    }

    /**
     * Get if this tag should preserve whitespace within child text nodes.
     *
     * @return if preserve whitespace
     */
    public boolean preserveWhitespace() {
        return preserveWhitespace;
    }

    /**
     * Get if this tag represents a control associated with a form. E.g. input, textarea, output
     * @return if associated with a form
     */
    public boolean isFormListed() {
        return formList;
    }

    /**
     * Get if this tag represents an element that should be submitted with a form. E.g. input, option
     * @return if submittable with a form
     */
    public boolean isFormSubmittable() {
        return formSubmit;
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

        if (!tagName.equals(tag.tagName)) return false;
        if (empty != tag.empty) return false;
        if (formatAsBlock != tag.formatAsBlock) return false;
        if (isBlock != tag.isBlock) return false;
        if (preserveWhitespace != tag.preserveWhitespace) return false;
        if (selfClosing != tag.selfClosing) return false;
        if (formList != tag.formList) return false;
        return formSubmit == tag.formSubmit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagName, isBlock, formatAsBlock, empty, selfClosing, preserveWhitespace,
            formList, formSubmit);
    }

    @Override
    public String toString() {
        return tagName;
    }

    @Override
    protected Tag clone() {
        try {
            return (Tag) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    // internal static initialisers:
    // prepped from http://www.w3.org/TR/REC-html40/sgml/dtd.html and other sources
    private static final String[] blockTags = {
            "html", "head", "body", "frameset", "script", "noscript", "style", "meta", "link", "title", "frame",
            "noframes", "section", "nav", "aside", "hgroup", "header", "footer", "p", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "pre", "div", "blockquote", "hr", "address", "figure", "figcaption", "form", "fieldset", "ins",
            "del", "dl", "dt", "dd", "li", "table", "caption", "thead", "tfoot", "tbody", "colgroup", "col", "tr", "th",
            "td", "video", "audio", "canvas", "details", "menu", "plaintext", "template", "article", "main",
            "svg", "math", "center", "template",
            "dir", "applet", "marquee", "listing" // deprecated but still known / special handling
    };
    private static final String[] inlineTags = {
            "object", "base", "font", "tt", "i", "b", "u", "big", "small", "em", "strong", "dfn", "code", "samp", "kbd",
            "var", "cite", "abbr", "time", "acronym", "mark", "ruby", "rt", "rp", "rtc", "a", "img", "br", "wbr", "map", "q",
            "sub", "sup", "bdo", "iframe", "embed", "span", "input", "select", "textarea", "label", "optgroup",
            "option", "legend", "datalist", "keygen", "output", "progress", "meter", "area", "param", "source", "track",
            "summary", "command", "device", "area", "basefont", "bgsound", "menuitem", "param", "source", "track",
            "data", "bdi", "s", "strike", "nobr",
            "rb", // deprecated but still known / special handling
            "text", // in SVG NS
            "mi", "mo", "msup", "mn", "mtext" // in MathML NS, to ensure inline
    };
    private static final String[] emptyTags = {
            "meta", "link", "base", "frame", "img", "br", "wbr", "embed", "hr", "input", "keygen", "col", "command",
            "device", "area", "basefont", "bgsound", "menuitem", "param", "source", "track"
    };
    // todo - rework this to format contents as inline; and update html emitter in Element. Same output, just neater.
    private static final String[] formatAsInlineTags = {
            "title", "a", "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "li", "th", "td", "script", "style",
            "ins", "del", "s", "button"
    };
    private static final String[] preserveWhitespaceTags = {
            "pre", "plaintext", "title", "textarea"
            // script is not here as it is a data node, which always preserve whitespace
    };
    // todo: I think we just need submit tags, and can scrub listed
    private static final String[] formListedTags = {
            "button", "fieldset", "input", "keygen", "object", "output", "select", "textarea"
    };
    private static final String[] formSubmitTags = SharedConstants.FormSubmitTags;

    private static final Map<String, String[]> namespaces = new HashMap<>();
    static {
        namespaces.put(Parser.NamespaceMathml, new String[]{"math", "mi", "mo", "msup", "mn", "mtext"});
        namespaces.put(Parser.NamespaceSvg, new String[]{"svg", "text"});
        // We don't need absolute coverage here as other cases will be inferred by the HtmlTreeBuilder
    }

    private static void setupTags(String[] tagNames, Consumer<Tag> tagModifier) {
        for (String tagName : tagNames) {
            Tag tag = Tags.get(tagName);
            if (tag == null) {
                tag = new Tag(tagName, Parser.NamespaceHtml);
                Tags.put(tag.tagName, tag);
            }
            tagModifier.accept(tag);
        }
    }

    static {
        setupTags(blockTags, tag -> {
            tag.isBlock = true;
            tag.formatAsBlock = true;
        });

        setupTags(inlineTags, tag -> {
            tag.isBlock = false;
            tag.formatAsBlock = false;
        });

        setupTags(emptyTags, tag -> tag.empty = true);
        setupTags(formatAsInlineTags, tag -> tag.formatAsBlock = false);
        setupTags(preserveWhitespaceTags, tag -> tag.preserveWhitespace = true);
        setupTags(formListedTags, tag -> tag.formList = true);
        setupTags(formSubmitTags, tag -> tag.formSubmit = true);
        for (Map.Entry<String, String[]> ns : namespaces.entrySet()) {
            setupTags(ns.getValue(), tag -> tag.namespace = ns.getKey());
        }
    }
}
