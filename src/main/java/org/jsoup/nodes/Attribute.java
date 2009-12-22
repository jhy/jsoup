package org.jsoup.nodes;

import org.apache.commons.lang.Validate;

/**
 A single key + value attribute.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Attribute {
    private String key;
    private String value;

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
}
