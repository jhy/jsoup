package org.jsoup.parser;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

import static org.jsoup.parser.Parser.NamespaceHtml;

/**
 * Tag capabilities.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class Tag implements Cloneable {
    // tag option constants
    public static int Defined = 1 << 0; // tag was defined by the TagSet
    public static int Void = 1 << 1; // void tag (e.g. <img>)
    public static int Inline = 1 << 2; // inline tag (e.g. <b>), will not indent self (vs Block, which will) // todo consider having both isBlock and isInline separate- both off means we infer (and get rid of FormatAsBlock)
    public static int SelfClose = 1 << 3; // can self close (e.g. <foo />)
    public static int SeenSelfClose = 1 << 4; // seen self close in this parse (e.g. <foo />)
    public static int PreserveWhitespace = 1 << 5; // preserve whitespace (e.g. <pre>)
    public static int RcData = 1 << 6; // RCDATA elements can have text and character references. E.g. title, textarea.
    public static int Data = 1 << 7; // Data elements can have text (and not character references). E.g. style, script.
    public static int FormSubmittable = 1 << 8; // form submittable (e.g. <input>)
    // todo remove after refactor:
    public static int FormatAsBlock = 1 << 9;
    public static int FormListed = 1 << 10;

    final String namespace;
    String tagName;
    final String normalName; // always the lower case version of this tag, regardless of case preservation mode
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

    /** Path for TagSet defaults, no options set; normal name is already LC. */
    Tag(String tagName, String normalName, String namespace) {
        this.tagName = tagName;
        this.normalName = normalName;
        this.namespace = namespace;
        set(Tag.Inline);
        set(Tag.FormatAsBlock); // todo cleanup printer
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
     Set an option on this tag.
     @param option the option to set
     @return this tag
     @since 1.20.1
     */
    public Tag set(int option) {
        options |= option;
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
        return (options & Inline) == 0;
    }

    /**
     * Gets if this tag should be formatted as a block (or as inline)
     *
     * @return if should be formatted as block or inline
     */
    public boolean formatAsBlock() {
        return (options & FormatAsBlock) != 0;
    }

    /**
     * Gets if this tag is an inline tag.
     *
     * @return if this tag is an inline tag.
     */
    public boolean isInline() {
        return (options & Inline) != 0;
    }

    /**
     * Get if this is an empty tag
     *
     * @return if this is an empty tag
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
        return (options & Defined) != 0;
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
     * Get if this tag represents a control associated with a form. E.g. input, textarea, output
     * @return if associated with a form
     */
    public boolean isFormListed() {
        return (options & FormListed) != 0;
    }

    /**
     * Get if this tag represents an element that should be submitted with a form. E.g. input, option
     * @return if submittable with a form
     */
    public boolean isFormSubmittable() {
        return (options &= FormSubmittable) != 0;
    }

    Tag setSelfClosing() {
        set(SelfClose);
        return this;
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
