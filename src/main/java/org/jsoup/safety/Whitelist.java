package org.jsoup.safety;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;

public class Whitelist {
    private Set<TagName> tagNames; // tags allowed, lower case. e.g. [p, br, span]
    private Map<TagName, Set<AttributeKey>> attributes; // tag -> attribute[]. allowed attributes [href, src, class] for a tag.
    private Map<TagName, Map<AttributeKey, AttributeValue>> addAttributes; // always set these attribute values
    private Map<TagName, Map<AttributeKey, Protocol>> protocols; // allowed URL protocols for attributes
    
    public Whitelist() {
        tagNames = new HashSet<TagName>();
        attributes = new HashMap<TagName, Set<AttributeKey>>();
        addAttributes = new HashMap<TagName, Map<AttributeKey, AttributeValue>>();
        protocols = new HashMap<TagName, Map<AttributeKey, Protocol>>();
    }
    
    public Whitelist addTags(String... tags) {
        for (String tagName : tags) {
            tagNames.add(TagName.valueOf(tagName));
        }
        return this;
    }
    
    public Whitelist addAttributes(String tag, String... attribute) {
        TagName tagName = TagName.valueOf(tag);
        Set<AttributeKey> attributeSet = new HashSet<AttributeKey>();
        for (String key : attribute) {
            attributeSet.add(AttributeKey.valueOf(key));
        }
        if (attributes.containsKey(tagName)) {
            Set<AttributeKey> currentSet = attributes.get(tagName);
            currentSet.addAll(attributeSet);
        } else {
            attributes.put(tagName, attributeSet);
        }
        return this;
        
    }

    // named types for config. All just hold strings, but here for my sanity.
    static class TagName extends TypedValue {
        TagName(String value) {
            super(value);
        }
        static TagName valueOf(String value) {
            return new TagName(value);
        }
    }
    
    static class AttributeKey extends TypedValue {
        AttributeKey(String value) {
            super(value);
        }
        static AttributeKey valueOf(String value) {
            return new AttributeKey(value);
        }
    }
    
    static class AttributeValue extends TypedValue {
        AttributeValue(String value) {
            super(value);
        }
        static AttributeValue valueOf(String value) {
            return new AttributeValue(value);
        }
    }
    
    static class Protocol extends TypedValue {
        Protocol(String value) {
            super(value);
        }
        static Protocol valueOf(String value) {
            return new Protocol(value);
        }
    }
    
    abstract static class TypedValue {
        private String value;
        
        TypedValue(String value) {
            Validate.notEmpty(value);
            this.value = value;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TypedValue other = (TypedValue) obj;
            if (value == null) {
                if (other.value != null) return false;
            } else if (!value.equals(other.value)) return false;
            return true;
        }
        
        @Override
        public String toString() {
            return value;
        }        
    }
}

