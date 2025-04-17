package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.SharedConstants;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.jsoup.parser.Parser.NamespaceHtml;
import static org.jsoup.parser.Parser.NamespaceMathml;
import static org.jsoup.parser.Parser.NamespaceSvg;

/**
 A TagSet controls the {@link Tag} configuration for a Document's parse, and its serialization. It contains the initial
 defaults, and after the parse, any additionally discovered tags.

 @see Parser#tagSet(TagSet)
 @since 1.20.1
 */
public class TagSet {
    static final TagSet HtmlTagSet = initHtmlDefault();

    final Map<String, Map<String, Tag>> tags = new HashMap<>(); // namespace -> tag name -> Tag
    final @Nullable TagSet source; // source to pull tags from on demand

    /**
     Returns a mutable copy of the default HTML tag set.
     */
    public static TagSet Html() {
        return new TagSet(HtmlTagSet);
    }

    public TagSet() {
        source = null;
    }

    public TagSet(TagSet original) {
        this.source = original;
    }

    /**
     Insert a tag into this TagSet. If the tag already exists, it is replaced.
     <p>Tags explicitly added like this are considered to be known tags (vs those that are dynamically created via
     .valueOf() if not already in the set.</p>

     @param tag the tag to add
     @return this TagSet
     */
    public TagSet add(Tag tag) {
        tag.set(Tag.Known);
        doAdd(tag);
        return this;
    }

    /** Adds the tag, but does not set defined. Used in .valueOf */
    private void doAdd(Tag tag) {
        tags.computeIfAbsent(tag.namespace, ns -> new HashMap<>())
            .put(tag.tagName, tag);
    }

    /**
     Get an existing Tag from this TagSet by tagName and namespace. The tag name is not normalized, to support mixed
     instances.

     @param tagName the case-sensitive tag name
     @param namespace the namespace
     @return the tag, or null if not found
     */
    public @Nullable Tag get(String tagName, String namespace) {
        Validate.notNull(tagName);
        Validate.notNull(namespace);

        // get from our tags
        Map<String, Tag> nsTags = tags.get(namespace);
        if (nsTags != null) {
            Tag tag = nsTags.get(tagName);
            if (tag != null) {
                return tag;
            }
        }

        // not found; clone on demand from source if exists
        if (source != null) {
            Tag tag = source.get(tagName, namespace);
            if (tag != null) {
                Tag copy = tag.clone();
                doAdd(copy);
                return copy;
            }
        }

        return null;
    }

    /** Tag.valueOf with the normalName via the token.normalName, to save redundant lower-casing passes. */
    Tag valueOf(String tagName, String normalName, String namespace, boolean preserveTagCase) {
        Validate.notNull(tagName);
        Validate.notNull(namespace);
        tagName = tagName.trim();
        Validate.notEmpty(tagName);
        Tag tag = get(tagName, namespace);
        if (tag != null) return tag;

        // not found by tagName, try by normal
        tagName = preserveTagCase ? tagName : normalName;
        tag = get(normalName, namespace);
        if (tag != null) {
            if (preserveTagCase && !tagName.equals(normalName)) {
                tag = tag.clone(); // copy so that the name update doesn't reset all instances
                tag.tagName = tagName;
                doAdd(tag);
            }
            return tag;
        }

        // not defined: return a new one
        tag = new Tag(tagName, normalName, namespace);
        doAdd(tag);

        return tag;
    }

    /**
     Get a Tag by name from this TagSet. If not previously defined (unknown), returns a new tag.
     <p>New tags will be added to this TagSet.</p>

     @param tagName Name of tag, e.g. "p".
     @param namespace the namespace for the tag.
     @param settings used to control tag name sensitivity
     @return The tag, either defined or new generic.
     */
    public Tag valueOf(String tagName, String namespace, ParseSettings settings) {
        return valueOf(tagName, ParseSettings.normalName(tagName), namespace, settings.preserveTagCase());
    }

    /**
     Get a Tag by name from this TagSet. If not previously defined (unknown), returns a new tag.
     <p>New tags will be added to this TagSet.</p>

     @param tagName Name of tag, e.g. "p". <b>Case-sensitive</b>.
     @param namespace the namespace for the tag.
     @return The tag, either defined or new generic.
     @see #valueOf(String tagName, String namespace, ParseSettings settings)
     */
    public Tag valueOf(String tagName, String namespace) {
        return valueOf(tagName, namespace, ParseSettings.preserveCase);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TagSet)) return false;
        TagSet tagSet = (TagSet) o;
        return Objects.equals(tags, tagSet.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tags);
    }

    // Default HTML initialization

    /**
     Initialize the default HTML tag set.
     */
    static TagSet initHtmlDefault() {
        String[] blockTags = {
            "html", "head", "body", "frameset", "script", "noscript", "style", "meta", "link", "title", "frame",
            "noframes", "section", "nav", "aside", "hgroup", "header", "footer", "p", "h1", "h2", "h3", "h4", "h5",
            "h6", "br", "button",
            "ul", "ol", "pre", "div", "blockquote", "hr", "address", "figure", "figcaption", "form", "fieldset", "ins",
            "del", "dl", "dt", "dd", "li", "table", "caption", "thead", "tfoot", "tbody", "colgroup", "col", "tr", "th",
            "td", "video", "audio", "canvas", "details", "menu", "plaintext", "template", "article", "main",
            "center", "template",
            "dir", "applet", "marquee", "listing", // deprecated but still known / special handling
            "#root" // the outer Document
        };
        String[] inlineTags = {
            "object", "base", "font", "tt", "i", "b", "u", "big", "small", "em", "strong", "dfn", "code", "samp", "kbd",
            "var", "cite", "abbr", "time", "acronym", "mark", "ruby", "rt", "rp", "rtc", "a", "img", "wbr", "map",
            "q",
            "sub", "sup", "bdo", "iframe", "embed", "span", "input", "select", "textarea", "label", "optgroup",
            "option", "legend", "datalist", "keygen", "output", "progress", "meter", "area", "param", "source", "track",
            "summary", "command", "device", "area", "basefont", "bgsound", "menuitem", "param", "source", "track",
            "data", "bdi", "s", "strike", "nobr",
            "rb", // deprecated but still known / special handling
        };
        String[] inlineContainers = { // can only contain inline; aka phrasing content
            "title", "a", "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "li", "th", "td", "script", "style",
            "ins", "del", "s", "button"
        };
        String[] voidTags = {
            "meta", "link", "base", "frame", "img", "br", "wbr", "embed", "hr", "input", "keygen", "col", "command",
            "device", "area", "basefont", "bgsound", "menuitem", "param", "source", "track"
        };
        String[] preserveWhitespaceTags = {
            "pre", "plaintext", "title", "textarea", "script"
        };
        String[] rcdataTags = { "title", "textarea" };
        String[] dataTags = { "iframe", "noembed", "noframes", "script", "style", "xmp" };
        String[] formSubmitTags = SharedConstants.FormSubmitTags;
        String[] blockMathTags = {"math"};
        String[] inlineMathTags = {"mi", "mo", "msup", "mn", "mtext"};
        String[] blockSvgTags = {"svg", "femerge", "femergenode"}; // note these are LC versions, but actually preserve case
        String[] inlineSvgTags = {"text"};

        return new TagSet()
            .setupTags(NamespaceHtml, blockTags, tag -> tag.set(Tag.Block))
            .setupTags(NamespaceHtml, inlineTags, tag -> tag.set(0))
            .setupTags(NamespaceHtml, inlineContainers, tag -> tag.set(Tag.InlineContainer))
            .setupTags(NamespaceHtml, voidTags, tag -> tag.set(Tag.Void))
            .setupTags(NamespaceHtml, preserveWhitespaceTags, tag -> tag.set(Tag.PreserveWhitespace))
            .setupTags(NamespaceHtml, rcdataTags, tag -> tag.set(Tag.RcData))
            .setupTags(NamespaceHtml, dataTags, tag -> tag.set(Tag.Data))
            .setupTags(NamespaceHtml, formSubmitTags, tag -> tag.set(Tag.FormSubmittable))
            .setupTags(NamespaceMathml, blockMathTags, tag -> tag.set(Tag.Block))
            .setupTags(NamespaceMathml, inlineMathTags, tag -> tag.set(0))
            .setupTags(NamespaceSvg, blockSvgTags, tag -> tag.set(Tag.Block))
            .setupTags(NamespaceSvg, inlineSvgTags, tag -> tag.set(0))
            ;
    }

    private TagSet setupTags(String namespace, String[] tagNames, Consumer<Tag> tagModifier) {
        for (String tagName : tagNames) {
            Tag tag = get(tagName, namespace);
            if (tag == null) {
                tag = new Tag(tagName, tagName, namespace); // normal name is already normal here
                tag.options = 0; // clear defaults
                add(tag);
            }
            tagModifier.accept(tag);
        }
        return this;
    }
}
