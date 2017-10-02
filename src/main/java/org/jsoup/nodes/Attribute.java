package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

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
    private String val;
    Attributes parent; // used to update the holding Attributes when the key / value is changed via this interface

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param value attribute value
     * @see #createFromEncoded
     */
    public Attribute(String key, String value) {
        this(key, value, null);
    }

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param val attribute value
     * @param parent the containing Attributes (this Attribute is not automatically added to said Attributes)
     * @see #createFromEncoded*/
    public Attribute(String key, String val, Attributes parent) {
        Validate.notNull(key);
        this.key = key.trim();
        Validate.notEmpty(key); // trimming could potentially make empty, so validate here
        this.val = val;
        this.parent = parent;
    }

    /**
     Get the attribute key.
     @return the attribute key
     */
    public String getKey() {
        return key;
    }

    /**
     Set the attribute key; case is preserved.
     @param key the new key; must not be null
     */
    public void setKey(String key) {
        Validate.notEmpty(key);
        key = key.trim();
        if (parent != null) {
            int i = parent.indexOfKey(this.key);
            if (i != Attributes.NotFound)
                parent.keys[i] = key;
        }
        this.key = key;
    }

    /**
     Get the attribute value.
     @return the attribute value
     */
    public String getValue() {
        return val;
    }

    /**
     Set the attribute value.
     @param val the new attribute value; must not be null
     */
    public String setValue(String val) {
        String oldVal = parent.get(this.key);
        if (parent != null) {
            int i = parent.indexOfKey(this.key);
            if (i != Attributes.NotFound)
                parent.vals[i] = val;
        }
        this.val = val;
        return oldVal;
    }

    /**
     Get the HTML representation of this attribute; e.g. {@code href="index.html"}.
     @return HTML
     */
    public String html() {
        StringBuilder accum = new StringBuilder();
        
        try {
        	html(accum, (new Document("")).outputSettings());
        } catch(IOException exception) {
        	throw new SerializationException(exception);
        }
        return accum.toString();
    }

    protected static void html(String key, String val, Appendable accum, Document.OutputSettings out) throws IOException {
        accum.append(key);
        if (!shouldCollapseAttribute(key, val, out)) {
            accum.append("=\"");
            Entities.escape(accum, Attributes.checkNotNull(val) , out, true, false, false);
            accum.append('"');
        }
    }
    
    protected void html(Appendable accum, Document.OutputSettings out) throws IOException {
        html(key, val, accum, out);
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

    protected static boolean shouldCollapseAttribute(String key, String val, Document.OutputSettings out) {
        // todo: optimize
        return (val == null || "".equals(val) || val.equalsIgnoreCase(key))
            && out.syntax() == Document.OutputSettings.Syntax.html
            && isBooleanAttribute(key);
    }

    /**
     * @deprecated
     */
    protected boolean isBooleanAttribute() {
        return Arrays.binarySearch(booleanAttributes, key) >= 0 || val == null;
    }

    /**
     * Checks if this attribute name is defined as a boolean attribute in HTML5
     */
    protected static boolean isBooleanAttribute(final String key) {
        return Arrays.binarySearch(booleanAttributes, key) >= 0;
    }

    @Override
    public boolean equals(Object o) { // note parent not considered
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        if (key != null ? !key.equals(attribute.key) : attribute.key != null) return false;
        return val != null ? val.equals(attribute.val) : attribute.val == null;
    }

    @Override
    public int hashCode() { // note parent not considered
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (val != null ? val.hashCode() : 0);
        return result;
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
