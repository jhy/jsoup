package org.jsoup.nodes;

import org.apache.commons.lang.Validate;

import java.util.*;

/**
 Element attribute list.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Attributes implements Iterable<Attribute> {
    private LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>(); // linked hash map to preserve insertion order.

    public String get(String key) {
        Validate.notEmpty(key);
        return attributes.get(key.toLowerCase());
    }

    public void put(String key, String value) {
        Validate.notEmpty(key);
        Validate.notNull(value);
        attributes.put(key.toLowerCase().trim(), value);
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

    public void mergeAttributes(Attributes incoming) {
        for (Attribute attribute : incoming) {
            this.put(attribute);
        }
    }

    public Iterator<Attribute> iterator() {
        return asList().iterator();
    }

    public List<Attribute> asList() {
        List<Attribute> list = new ArrayList<Attribute>(attributes.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            Attribute attribute = new Attribute(entry.getKey(), entry.getValue());
            list.add(attribute);
        }
        return Collections.unmodifiableList(list);
    }

    // todo: toString


}
