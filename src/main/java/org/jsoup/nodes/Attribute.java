package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 A single key + value attribute. Keys are trimmed and normalised to lower-case.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Attribute implements Map.Entry<String, String>, Cloneable  {
    private static final String[] booleanAttributes = {
            "allowfullscreen", "async", "autofocus", "checked", "compact", "declare", "default", "defer", "disabled",
            "formnovalidate", "hidden", "inert", "ismap", "itemscope", "multiple", "muted", "nohref", "noresize",
            "noshade", "novalidate", "nowrap", "open", "readonly", "required", "reversed", "seamless", "selected",
            "sortable", "truespeed", "typemustmatch"
    };

    private String key;
    private String value;

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param value attribute value
     * @see #createFromEncoded
     */
    public Attribute(String key, String value) {
        Validate.notEmpty(key);
        Validate.notNull(value);
        this.key = key.trim();
        this.value = value;
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
        this.key = key.trim();
    }

    /**
     Get the attribute value.
     @return the attribute value
     */
    public String getValue() {
        return value;
    }

    /**
     Set the attribute value.
     @param value the new attribute value; must not be null
     */
    public String setValue(String value) {
        Validate.notNull(value);
        String old = this.value;
        this.value = value;
        return old;
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
    
    protected void html(Appendable accum, Document.OutputSettings out) throws IOException {
        accum.append(key);
        if (!shouldCollapseAttribute(out)) {
            accum.append("=\"");
            Entities.escape(accum, value, out, true, false, false);
            accum.append('"');
        }
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
        return new Attribute(unencodedKey, value);
    }

    protected boolean isDataAttribute() {
        return key.startsWith(Attributes.dataPrefix) && key.length() > Attributes.dataPrefix.length();
    }

    /**
     * Collapsible if it's a boolean attribute and value is empty or same as name
     * 
     * @param out Outputsettings
     * @return  Returns whether collapsible or not
     */
    protected final boolean shouldCollapseAttribute(Document.OutputSettings out) {
        return ("".equals(value) || value.equalsIgnoreCase(key))
                && out.syntax() == Document.OutputSettings.Syntax.html
                && isBooleanAttribute();
    }

    protected boolean isBooleanAttribute() {
        return Arrays.binarySearch(booleanAttributes, key) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attribute)) return false;

        Attribute attribute = (Attribute) o;

        if (key != null ? !key.equals(attribute.key) : attribute.key != null) return false;
        return !(value != null ? !value.equals(attribute.value) : attribute.value != null);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public Attribute clone() {
        try {
            return (Attribute) super.clone(); // only fields are immutable strings key and value, so no more deep copy required
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
