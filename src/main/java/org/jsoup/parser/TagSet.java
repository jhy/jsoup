package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.SharedConstants;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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

    private final Map<String, Map<String, Tag>> tags = new HashMap<>(); // namespace -> tag name -> Tag
    private final @Nullable TagSet source; // internal fallback for lazy tag copies
    private @Nullable ArrayList<Consumer<Tag>> customizers; // optional onNewTag tag customizer

    /**
     Returns a mutable copy of the default HTML tag set.
     */
    public static TagSet Html() {
        return new TagSet(HtmlTagSet, null);
    }

    private TagSet(@Nullable TagSet source, @Nullable ArrayList<Consumer<Tag>> customizers) {
        this.source = source;
        this.customizers = customizers;
    }

    public TagSet() {
        this(null, null);
    }

    /**
     Creates a new TagSet by copying the current tags and customizers from the provided source TagSet. Changes made to
     one TagSet will not affect the other.
     @param template the TagSet to copy
     */
    public TagSet(TagSet template) {
        this(template.source, copyCustomizers(template));
        // copy tags eagerly; any lazy pull-through should come only from the root source (which would be the HTML defaults), not the template itself.
        // that way the template tagset is not mutated when we do read through
        if (template.tags.isEmpty()) return;

        for (Map.Entry<String, Map<String, Tag>> namespaceEntry : template.tags.entrySet()) {
            Map<String, Tag> nsTags = new HashMap<>(namespaceEntry.getValue().size());
            for (Map.Entry<String, Tag> tagEntry : namespaceEntry.getValue().entrySet()) {
                nsTags.put(tagEntry.getKey(), tagEntry.getValue().clone());
            }
            tags.put(namespaceEntry.getKey(), nsTags);
        }
    }

    private static @Nullable ArrayList<Consumer<Tag>> copyCustomizers(TagSet base) {
        if (base.customizers == null) return null;
        return new ArrayList<>(base.customizers);
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
        if (customizers != null) {
            for (Consumer<Tag> customizer : customizers) {
                customizer.accept(tag);
            }
        }

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

    /**
     Tag.valueOf with the normalName via the token.normalName, to save redundant lower-casing passes.
     Provide a null normalName unless we already have one; will be normalized if required from tagName.
     */
    Tag valueOf(String tagName, @Nullable String normalName, String namespace, boolean preserveTagCase) {
        Validate.notNull(tagName);
        Validate.notNull(namespace);
        tagName = tagName.trim();
        Validate.notEmpty(tagName);
        Tag tag = get(tagName, namespace);
        if (tag != null) return tag;

        // not found by tagName, try by normal
        if (normalName == null) normalName = ParseSettings.normalName(tagName);
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
        return valueOf(tagName, null, namespace, settings.preserveTagCase());
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

    /**
     Register a callback to customize each {@link Tag} as it's added to this TagSet.
     <p>Customizers are invoked once per Tag, when they are added (explicitly or via the valueOf methods).</p>

     <p>For example, to allow all unknown tags to be self-closing during when parsing as HTML:</p>
     <pre><code>
     Parser parser = Parser.htmlParser();
     parser.tagSet().onNewTag(tag -> {
     if (!tag.isKnownTag())
        tag.set(Tag.SelfClose);
     });

     Document doc = Jsoup.parse(html, parser);
     </code></pre>

     @param customizer a {@code Consumer<Tag>} that will be called for each newly added or cloned Tag; callers can
     inspect and modify the Tag's state (e.g. set options)
     @return this TagSet, to allow method chaining
     @since 1.21.0
     */
    public TagSet onNewTag(Consumer<Tag> customizer) {
        Validate.notNull(customizer);
        if (customizers == null)
            customizers = new ArrayList<>();
        customizers.add(customizer);
        return this;
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
            "h6", "button",
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
        String[] dataSvgTags = {"script"};

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
            .setupTags(NamespaceSvg, dataSvgTags, tag -> tag.set(Tag.Data))
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
