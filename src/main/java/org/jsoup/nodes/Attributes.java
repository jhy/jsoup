package org.jsoup.nodes;

import org.apache.commons.lang.Validate;

import java.util.*;

/**
 The attributes of an Element.
 <p/>
 Attributes are treated as a map: there can be only one value associated with an attribute key.
 <p/>
 Attribute key and value comparisons are done case insensitively, and keys are normalised to lower-case.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Attributes implements Iterable<Attribute> {
    private LinkedHashMap<String, Attribute> attributes = new LinkedHashMap<String, Attribute>(); // linked hash map to preserve insertion order.

    public String get(String key) {
        Validate.notEmpty(key);
        
        Attribute attr = attributes.get(key.toLowerCase());
        return attr != null ? attr.getValue() : "";
    }

    public void put(String key, String value) {
        Attribute attr = new Attribute(key, value);
        put(attr);
    }

    public void put(Attribute attribute) {
        Validate.notNull(attribute);
        attributes.put(attribute.getKey(), attribute);
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
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            list.add(entry.getValue());
        }
        return Collections.unmodifiableList(list);
    }

    // todo: toString


}
