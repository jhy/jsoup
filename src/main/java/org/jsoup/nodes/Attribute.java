package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.jsoup.internal.Normalizer;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 A single key + value attribute. (Only used for presentation.)
 */
public class Attribute implements Map.Entry<String, String>, Cloneable  {
    private static final String[] booleanAttributes = {
            "allowfullscreen", "async", "autofocus", "checked", "compact", "declare", "default", "defer", "disabled",
            "formnovalidate", "hidden", "inert", "ismap", "itemscope", "multiple", "muted", "nohref", "noresize",
            "noshade", "novalidate", "nowrap", "open", "readonly", "required", "reversed", "seamless", "selected",
            "sortable", "truespeed", "typemustmatch"
    };

    private String key;
    @Nullable private String val;
    @Nullable Attributes parent; // used to update the holding Attributes when the key / value is changed via this interface

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param value attribute value (may be null)
     * @see #createFromEncoded
     */
    public Attribute(String key, @Nullable String value) {
        this(key, value, null);
    }

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param val attribute value (may be null)
     * @param parent the containing Attributes (this Attribute is not automatically added to said Attributes)
     * @see #createFromEncoded*/
    public Attribute(String key, @Nullable String val, @Nullable Attributes parent) {
        Validate.notNull(key);
        key = key.trim();
        Validate.notEmpty(key); // trimming could potentially make empty, so validate here
        this.key = key;
        this.val = val;
        this.parent = parent;
    }

    /**
     Get the attribute key.
     @return the attribute key
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     Set the attribute key; case is preserved.
     @param key the new key; must not be null
     */
    public void setKey(String key) {
        Validate.notNull(key);
        key = key.trim();
        Validate.notEmpty(key); // trimming could potentially make empty, so validate here
        if (parent != null) {
            int i = parent.indexOfKey(this.key);
            if (i != Attributes.NotFound) {
                String oldKey = parent.keys[i];
                parent.keys[i] = key;

                // if tracking source positions, update the key in the range map
                Map<String, Range.AttributeRange> ranges = parent.getRanges();
                if (ranges != null) {
                    Range.AttributeRange range = ranges.remove(oldKey);
                    ranges.put(key, range);
                }
            }
        }
        this.key = key;
    }

    /**
     Get the attribute value. Will return an empty string if the value is not set.
     @return the attribute value
     */
    @Override
    public String getValue() {
        return Attributes.checkNotNull(val);
    }

    /**
     * Check if this Attribute has a value. Set boolean attributes have no value.
     * @return if this is a boolean attribute / attribute without a value
     */
    public boolean hasDeclaredValue() {
        return val != null;
    }

    /**
     Set the attribute value.
     @param val the new attribute value; may be null (to set an enabled boolean attribute)
     @return the previous value (if was null; an empty string)
     */
    @Override public String setValue(@Nullable String val) {
        String oldVal = this.val;
        if (parent != null) {
            int i = parent.indexOfKey(this.key);
            if (i != Attributes.NotFound) {
                oldVal = parent.get(this.key); // trust the container more
                parent.vals[i] = val;
            }
        }
        this.val = val;
        return Attributes.checkNotNull(oldVal);
    }

    /**
     Get the HTML representation of this attribute; e.g. {@code href="index.html"}.
     @return HTML
     */
    public String html() {
        StringBuilder sb = StringUtil.borrowBuilder();
        
        try {
        	html(sb, (new Document("")).outputSettings());
        } catch(IOException exception) {
        	throw new SerializationException(exception);
        }
        return StringUtil.releaseBuilder(sb);
    }

    /**
     Get the source ranges (start to end positions) in the original input source from which this attribute's <b>name</b>
     and <b>value</b> were parsed.
     <p>Position tracking must be enabled prior to parsing the content.</p>
     @return the ranges for the attribute's name and value, or {@code untracked} if the attribute does not exist or its range
     was not tracked.
     @see org.jsoup.parser.Parser#setTrackPosition(boolean)
     @see Attributes#sourceRange(String)
     @see Node#sourceRange()
     @see Element#endSourceRange()
     @since 1.17.1
     */
    public Range.AttributeRange sourceRange() {
        if (parent == null) return Range.AttributeRange.UntrackedAttr;
        return parent.sourceRange(key);
    }

    protected void html(Appendable accum, Document.OutputSettings out) throws IOException {
        html(key, val, accum, out);
    }

    protected static void html(String key, @Nullable String val, Appendable accum, Document.OutputSettings out) throws IOException {
        key = getValidKey(key, out.syntax());
        if (key == null) return; // can't write it :(
        htmlNoValidate(key, val, accum, out);
    }

    static void htmlNoValidate(String key, @Nullable String val, Appendable accum, Document.OutputSettings out) throws IOException {
        // structured like this so that Attributes can check we can write first, so it can add whitespace correctly
        accum.append(key);
        if (!shouldCollapseAttribute(key, val, out)) {
            accum.append("=\"");
            Entities.escape(accum, Attributes.checkNotNull(val), out, Entities.ForAttribute); // preserves whitespace
            accum.append('"');
        }
    }

    private static final Pattern xmlKeyReplace = Pattern.compile("[^-a-zA-Z0-9_:.]+");
    private static final Pattern htmlKeyReplace = Pattern.compile("[\\x00-\\x1f\\x7f-\\x9f \"'/=]+");
    /**
     * Get a valid attribute key for the given syntax. If the key is not valid, it will be coerced into a valid key.
     * @param key the original attribute key
     * @param syntax HTML or XML
     * @return the original key if it's valid; a key with invalid characters replaced with "_" otherwise; or null if a valid key could not be created.
     */
    @Nullable public static String getValidKey(String key, Syntax syntax) {
        if (syntax == Syntax.xml && !isValidXmlKey(key)) {
            key = xmlKeyReplace.matcher(key).replaceAll("_");
            return isValidXmlKey(key) ? key : null; // null if could not be coerced
        }
        else if (syntax == Syntax.html && !isValidHtmlKey(key)) {
            key = htmlKeyReplace.matcher(key).replaceAll("_");
            return isValidHtmlKey(key) ? key : null; // null if could not be coerced
        }
        return key;
    }

    // perf critical in html() so using manual scan vs regex:
    // note that we aren't using anything in supplemental space, so OK to iter charAt
    private static boolean isValidXmlKey(String key) {
        // =~ [a-zA-Z_:][-a-zA-Z0-9_:.]*
        final int length = key.length();
        if (length == 0) return false;
        char c = key.charAt(0);
        if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == ':'))
            return false;
        for (int i = 1; i < length; i++) {
            c = key.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == ':' || c == '.'))
                return false;
        }
        return true;
    }

    private static boolean isValidHtmlKey(String key) {
        // =~ [\x00-\x1f\x7f-\x9f "'/=]+
        final int length = key.length();
        if (length == 0) return false;
        for (int i = 0; i < length; i++) {
            char c = key.charAt(i);
            if ((c <= 0x1f) || (c >= 0x7f && c <= 0x9f) || c == ' ' || c == '"' || c == '\'' || c == '/' || c == '=')
                return false;
        }
        return true;
    }

    /**
     Get the string representation of this attribute, implemented as {@link #html()}.
     @return string
     */
    @Override
    public String toString() {
        return html();
    }

    /**
     * Create a new Attribute from an unencoded key and a HTML attribute encoded value.
     * @param unencodedKey assumes the key is not encoded, as can be only run of simple \w chars.
     * @param encodedValue HTML attribute encoded value
     * @return attribute
     */
    public static Attribute createFromEncoded(String unencodedKey, String encodedValue) {
        String value = Entities.unescape(encodedValue, true);
        return new Attribute(unencodedKey, value, null); // parent will get set when Put
    }

    protected boolean isDataAttribute() {
        return isDataAttribute(key);
    }

    protected static boolean isDataAttribute(String key) {
        return key.startsWith(Attributes.dataPrefix) && key.length() > Attributes.dataPrefix.length();
    }

    /**
     * Collapsible if it's a boolean attribute and value is empty or same as name
     * 
     * @param out output settings
     * @return  Returns whether collapsible or not
     */
    protected final boolean shouldCollapseAttribute(Document.OutputSettings out) {
        return shouldCollapseAttribute(key, val, out);
    }

    // collapse unknown foo=null, known checked=null, checked="", checked=checked; write out others
    protected static boolean shouldCollapseAttribute(final String key, @Nullable final String val, final Document.OutputSettings out) {
        return (
            out.syntax() == Syntax.html &&
                (val == null || (val.isEmpty() || val.equalsIgnoreCase(key)) && Attribute.isBooleanAttribute(key)));
    }

    /**
     * Checks if this attribute name is defined as a boolean attribute in HTML5
     */
    public static boolean isBooleanAttribute(final String key) {
        return Arrays.binarySearch(booleanAttributes, Normalizer.lowerCase(key)) >= 0;
    }

    @Override
    public boolean equals(@Nullable Object o) { // note parent not considered
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return Objects.equals(key, attribute.key) && Objects.equals(val, attribute.val);
    }

    @Override
    public int hashCode() { // note parent not considered
        return Objects.hash(key, val);
    }

    @Override
    public Attribute clone() {
        try {
            return (Attribute) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
