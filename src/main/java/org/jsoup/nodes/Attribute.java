package org.jsoup.nodes;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;

/**
 A single key + value attribute.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Attribute {
    private String key;
    private String value;

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key
     * @param value attribute value
     * @see #createFromEncoded
     */
    public Attribute(String key, String value) {
        Validate.notEmpty(key);
        Validate.notNull(value);
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        Validate.notEmpty(key);
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        Validate.notNull(value);
        this.value = value;
    }

    /**
     * Create a new Attribute from an unencoded key and a HMTL attribute encoded value.
     * @param unencodedKey assumes the key is not encoded, as can be only run of simple \w chars.
     * @param encodedValue HTML attribute encoded value
     * @return attribute
     */
    public static Attribute createFromEncoded(String unencodedKey, String encodedValue) {
        String value = StringEscapeUtils.unescapeHtml(encodedValue);
        return new Attribute(unencodedKey, value);
    }
}
