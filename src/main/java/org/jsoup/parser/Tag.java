package org.jsoup.parser;

import org.jsoup.internal.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

import static org.jsoup.parser.Parser.NamespaceHtml;

/**
 A Tag represents an Element's name and configured options, common throughout the Document. Options may affect the parse
 and output.

 @see TagSet
 @see Parser#tagSet(TagSet) */
public class Tag implements Cloneable {
    /** Tag option: the tag is known (specifically defined). This impacts if options may need to be inferred (when not
     known) in, e.g., the pretty-printer. Set when a tag is added to a TagSet, or when settings are set(). */
    public static int Known = 1;
    /** Tag option: the tag is a void tag (e.g. {@code <img>}), that can contain no children, and in HTML does not require closing. */
    public static int Void = 1 << 1;
    /** Tag option: the tag is a block tag (e.g. {@code <div>}, {@code <p>}). Causes the element to be indented when pretty-printing. If not a block, it is inline. */
    public static int Block = 1 << 2;
    /** Tag option: the tag is a block tag that will only hold inline tags (e.g. {@code <p>}); used for formatting. (Must also set Block.) */
    public static int InlineContainer = 1 << 3;
    /** Tag option: the tag can self-close (e.g. <foo />). */
    public static int SelfClose = 1 << 4;
    /** Tag option: the tag has been seen self-closing in this parse. */
    public static int SeenSelfClose = 1 << 5;
    /** Tag option: the tag preserves whitespace (e.g. {@code <pre>}). */
    public static int PreserveWhitespace = 1 << 6;
    /** Tag option: the tag is an RCDATA element that can have text and character references (e.g. {@code <title>}, {@code <textarea>}). */
    public static int RcData = 1 << 7;
    /** Tag option: the tag is a Data element that can have text but not character references (e.g. {@code <style>}, {@code <script>}). */
    public static int Data = 1 << 8;
    /** Tag option: the tag's value will be included when submitting a form (e.g. {@code <input>}). */
    public static int FormSubmittable = 1 << 9;

    String namespace;
    String tagName;
    String normalName; // always the lower case version of this tag, regardless of case preservation mode
    int options = 0;

    /**
     Create a new Tag, with the given name and namespace.
     <p>The tag is not implicitly added to any TagSet.</p>
     @param tagName the name of the tag. Case-sensitive.
     @param namespace the namespace for the tag.
     @see TagSet#valueOf(String, String)
     @since 1.20.1
     */
    public Tag(String tagName, String namespace) {
        this(tagName, ParseSettings.normalName(tagName), namespace);
    }

    /**
     Create a new Tag, with the given name, in the HTML namespace.
     <p>The tag is not implicitly added to any TagSet.</p>
     @param tagName the name of the tag. Case-sensitive.
     @see TagSet#valueOf(String, String)
     @since 1.20.1
     */
    public Tag(String tagName) {
        this(tagName, ParseSettings.normalName(tagName), NamespaceHtml);
    }

    /** Path for TagSet defaults, no options set; normal name is already LC. */
    Tag(String tagName, String normalName, String namespace) {
        this.tagName = tagName;
        this.normalName = normalName;
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
     Get this tag's name.
     @return the tag's name
     */
    public String name() {
        return tagName;
    }

    /**
     Change the tag's name. As Tags are reused throughout a Document, this will change the name for all uses of this tag.
     @param tagName the new name of the tag. Case-sensitive.
     @return this tag
     @since 1.20.1
     */
    public Tag name(String tagName) {
        this.tagName = tagName;
        this.normalName = ParseSettings.normalName(tagName);
        return this;
    }

    /**
     Get this tag's prefix, if it has one; else the empty string.
     <p>For example, {@code <book:title>} has prefix {@code book}, and tag name {@code book:title}.</p>
     @return the tag's prefix
     @since 1.20.1
     */
    public String prefix() {
        int pos = tagName.indexOf(':');
        if (pos == -1) return "";
        else return tagName.substring(0, pos);
    }

    /**
     Get this tag's local name. The local name is the name without the prefix (if any).
     <p>For exmaple, {@code <book:title>} has local name {@code title}, and tag name {@code book:title}.</p>
     @return the tag's local name
     @since 1.20.1
     */
    public String localName() {
        int pos = tagName.indexOf(':');
        if (pos == -1) return tagName;
        else return tagName.substring(pos + 1);
    }

    /**
     * Get this tag's normalized (lowercased) name.
     * @return the tag's normal name.
     */
    public String normalName() {
        return normalName;
    }

    /**
     Get this tag's namespace.
     @return the tag's namespace
     */
    public String namespace() {
        return namespace;
    }

    /**
     Set the tag's namespace. As Tags are reused throughout a Document, this will change the namespace for all uses of this tag.
     @param namespace the new namespace of the tag.
     @return this tag
     @since 1.20.1
     */
    public Tag namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     Set an option on this tag.
     <p>Once a tag has a setting applied, it will be considered a known tag.</p>
     @param option the option to set
     @return this tag
     @since 1.20.1
     */
    public Tag set(int option) {
        options |= option;
        options |= Tag.Known; // considered known if touched
        return this;
    }

    /**
     Test if an option is set on this tag.

     @param option the option to test
     @return true if the option is set
     @since 1.20.1
     */
    public boolean is(int option) {
        return (options & option) != 0;
    }

    /**
     Clear (unset) an option from this tag.
     @param option the option to clear
     @return this tag
     @since 1.20.1
     */
    public Tag clear(int option) {
        options &= ~option;
        // considered known if touched, unless explicitly clearing known
        if (option != Tag.Known) options |= Tag.Known;
        return this;
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
     * @see TagSet
     * @return The tag, either defined or new generic.
     */
    public static Tag valueOf(String tagName, String namespace, ParseSettings settings) {
        return TagSet.Html().valueOf(tagName, ParseSettings.normalName(tagName), namespace, settings.preserveTagCase());
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
        return valueOf(tagName, NamespaceHtml, ParseSettings.preserveCase);
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
        return valueOf(tagName, NamespaceHtml, settings);
    }

    /**
     * Gets if this is a block tag.
     *
     * @return if block tag
     */
    public boolean isBlock() {
        return (options & Block) != 0;
    }

    /**
     Get if this is an InlineContainer tag.

     @return true if an InlineContainer (which formats children as inline).
     @deprecated setting is only used within the Printer. Will be removed in 1.21.
     */
    @Deprecated public boolean formatAsBlock() {
        return (options & InlineContainer) != 0;
    }

    /**
     * Gets if this tag is an inline tag. Just the opposite of isBlock.
     *
     * @return if this tag is an inline tag.
     */
    public boolean isInline() {
        return (options & Block) == 0;
    }

    /**
     Get if this is void (aka empty) tag.

     @return true if this is a void tag
     */
    public boolean isEmpty() {
        return (options & Void) != 0;
    }

    /**
     * Get if this tag is self-closing.
     *
     * @return if this tag should be output as self-closing.
     */
    public boolean isSelfClosing() {
        return (options & SelfClose) != 0 || (options & Void) != 0;
    }

    /**
     * Get if this is a pre-defined tag in the TagSet, or was auto created on parsing.
     *
     * @return if a known tag
     */
    public boolean isKnownTag() {
        return (options & Known) != 0;
    }

    /**
     * Check if this tag name is a known HTML tag.
     *
     * @param tagName name of tag
     * @return if known HTML tag
     */
    public static boolean isKnownTag(String tagName) {
        return TagSet.HtmlTagSet.get(tagName, NamespaceHtml) != null;
    }

    /**
     * Get if this tag should preserve whitespace within child text nodes.
     *
     * @return if preserve whitespace
     */
    public boolean preserveWhitespace() {
        return (options & PreserveWhitespace) != 0;
    }

    /**
     * Get if this tag represents an element that should be submitted with a form. E.g. input, option
     * @return if submittable with a form
     */
    public boolean isFormSubmittable() {
        return (options &= FormSubmittable) != 0;
    }

    void setSeenSelfClose() {
        options |= Tag.SeenSelfClose; // does not change known status
    }

    /**
     If this Tag uses a specific text TokeniserState for its content, returns that; otherwise null.
     */
    @Nullable TokeniserState textState() {
        if (is(RcData)) return TokeniserState.Rcdata;
        if (is(Data))   return TokeniserState.Rawtext;
        else            return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return Objects.equals(tagName, tag.tagName) &&
            Objects.equals(namespace, tag.namespace) &&
            Objects.equals(normalName, tag.normalName) &&
            options == tag.options;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagName, namespace, normalName, options);
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


}
