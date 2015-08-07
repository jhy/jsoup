package org.jsoup.nodes;

/**
 * A boolean attribute that is written out without any value.
 */
public class BooleanAttribute extends Attribute {
    /**
     * Create a new boolean attribute from unencoded (raw) key.
     * @param key attribute key
     */
    public BooleanAttribute(String key) {
        super(key, "");
    }
    
    /**
     * Create a new boolean attribute from unencoded (raw) key.
     * @param key attribute key
     * @paran quote attribute quote
     */
    public BooleanAttribute(String key, char quote) {
        super(key, "", quote);
    }
    
    @Override
    protected boolean isBooleanAttribute() {
        return true;
    }
}
