package org.jsoup.nodes;

import org.apache.commons.lang.Validate;

import java.util.LinkedHashMap;

/**
 Element attribute list.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Attributes {
    private LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>(); // linked hash map to preserve insertion order.

    public String get(String key) {
        Validate.notEmpty(key);
        return attributes.get(key.toLowerCase());
    }

    public void put(String key, String value) {
        Validate.notEmpty(key);
        Validate.notNull(value);
        attributes.put(key.toLowerCase().trim(), value.trim());
    }

    public void put(Attribute attribute) {
        Validate.notNull(attribute);
        put(attribute.getKey(), attribute.getValue());
    }

    public void remove(String key) {
        Validate.notEmpty(key);
        attributes.remove(key.toLowerCase());
    }

    public int size() {
        return attributes.size();
    }

    // todo: toString, list (as List<Attribute>)


}
