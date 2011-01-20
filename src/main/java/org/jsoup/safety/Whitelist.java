package org.jsoup.safety;

/*
    Thank you to Ryan Grove (wonko.com) for the Ruby HTML cleaner http://github.com/rgrove/sanitize/, which inspired
    this whitelist configuration, and the initial defaults.
 */

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 Whitelists define what HTML (elements and attributes) to allow through the cleaner. Everything else is removed.
 <p/>
 Start with one of the defaults:
 <ul>
 <li>{@link #none}
 <li>{@link #simpleText}
 <li>{@link #basic}
 <li>{@link #basicWithImages}
 <li>{@link #relaxed}
 </ul>
 <p/>
 If you need to allow more through (please be careful!), tweak a base whitelist with:
 <ul>
 <li>{@link #addTags}
 <li>{@link #addAttributes}
 <li>{@link #addEnforcedAttribute}
 <li>{@link #addProtocols}
 </ul>
 <p/>
 The cleaner and these whitelists assume that you want to clean a <code>body</code> fragment of HTML (to add user
 supplied HTML into a templated page), and not to clean a full HTML document. If the latter is the case, either wrap the
 document HTML around the cleaned body HTML, or create a whitelist that allows <code>html</code> and <code>head</code>
 elements as appropriate.
 <p/>
 If you are going to extend a whitelist, please be very careful. Make sure you understand what attributes may lead to
 XSS attack vectors. URL attributes are particularly vulnerable and require careful validation. See 
 http://ha.ckers.org/xss.html for some XSS attack examples.

 @author Jonathan Hedley
 */
public class Whitelist {
    private Set<TagName> tagNames; // tags allowed, lower case. e.g. [p, br, span]
    private Map<TagName, Set<AttributeKey>> attributes; // tag -> attribute[]. allowed attributes [href] for a tag.
    private Map<TagName, Map<AttributeKey, AttributeValue>> enforcedAttributes; // always set these attribute values
    private Map<TagName, Map<AttributeKey, Set<Protocol>>> protocols; // allowed URL protocols for attributes

    /**
     This whitelist allows only text nodes: all HTML will be stripped.

     @return whitelist
     */
    public static Whitelist none() {
        return new Whitelist();
    }

    /**
     This whitelist allows only simple text formatting: <code>b, em, i, strong, u</code>. All other HTML (tags and
     attributes) will be removed.

     @return whitelist
     */
    public static Whitelist simpleText() {
        return new Whitelist()
                .addTags("b", "em", "i", "strong", "u")
                ;
    }

    /**
     This whitelist allows a fuller range of text nodes: <code>a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li,
     ol, p, pre, q, small, strike, strong, sub, sup, u, ul</code>, and appropriate attributes.
     <p/>
     Links (<code>a</code> elements) can point to <code>http, https, ftp, mailto</code>, and have an enforced
     <code>rel=nofollow</code> attribute.
     <p/>
     Does not allow images.

     @return whitelist
     */
    public static Whitelist basic() {
        return new Whitelist()
                .addTags(
                        "a", "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
                        "i", "li", "ol", "p", "pre", "q", "small", "strike", "strong", "sub",
                        "sup", "u", "ul")

                .addAttributes("a", "href")
                .addAttributes("blockquote", "cite")
                .addAttributes("q", "cite")

                .addProtocols("a", "href", "ftp", "http", "https", "mailto")
                .addProtocols("blockquote", "cite", "http", "https")
                .addProtocols("cite", "cite", "http", "https")

                .addEnforcedAttribute("a", "rel", "nofollow")
                ;

    }

    /**
     This whitelist allows the same text tags as {@link #basic}, and also allows <code>img</code> tags, with appropriate
     attributes, with <code>src</code> pointing to <code>http</code> or <code>https</code>.

     @return whitelist
     */
    public static Whitelist basicWithImages() {
        return basic()
                .addTags("img")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addProtocols("img", "src", "http", "https")
                ;
    }

    /**
     This whitelist allows a full range of text and structural body HTML: <code>a, b, blockquote, br, caption, cite,
     code, col, colgroup, dd, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, small, strike, strong, sub,
     sup, table, tbody, td, tfoot, th, thead, tr, u, ul</code>
     <p/>
     Links do not have an enforced <code>rel=nofollow</code> attribute, but you can add that if desired.

     @return whitelist
     */
    public static Whitelist relaxed() {
        return new Whitelist()
                .addTags(
                        "a", "b", "blockquote", "br", "caption", "cite", "code", "col",
                        "colgroup", "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                        "i", "img", "li", "ol", "p", "pre", "q", "small", "strike", "strong",
                        "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u",
                        "ul")

                .addAttributes("a", "href", "title")
                .addAttributes("blockquote", "cite")
                .addAttributes("col", "span", "width")
                .addAttributes("colgroup", "span", "width")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addAttributes("ol", "start", "type")
                .addAttributes("q", "cite")
                .addAttributes("table", "summary", "width")
                .addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width")
                .addAttributes(
                        "th", "abbr", "axis", "colspan", "rowspan", "scope",
                        "width")
                .addAttributes("ul", "type")

                .addProtocols("a", "href", "ftp", "http", "https", "mailto")
                .addProtocols("blockquote", "cite", "http", "https")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("q", "cite", "http", "https")
                ;
    }

    /**
     Create a new, empty whitelist. Generally it will be better to start with a default prepared whitelist instead.

     @see #basic()
     @see #basicWithImages()
     @see #simpleText()
     @see #relaxed()
     */
    public Whitelist() {
        tagNames = new HashSet<TagName>();
        attributes = new HashMap<TagName, Set<AttributeKey>>();
        enforcedAttributes = new HashMap<TagName, Map<AttributeKey, AttributeValue>>();
        protocols = new HashMap<TagName, Map<AttributeKey, Set<Protocol>>>();
    }

    /**
     Add a list of allowed elements to a whitelist. (If a tag is not allowed, it will be removed from the HTML.)

     @param tags tag names to allow
     @return this (for chaining)
     */
    public Whitelist addTags(String... tags) {
        Validate.notNull(tags);

        for (String tagName : tags) {
            Validate.notEmpty(tagName);
            tagNames.add(TagName.valueOf(tagName));
        }
        return this;
    }

    /**
     Add a list of allowed attributes to a tag. (If an attribute is not allowed on an element, it will be removed.)
     <p/>
     To make an attribute valid for <b>all tags</b>, use the pseudo tag <code>:all</code>, e.g.
     <code>addAttributes(":all", "class")</code>.

     @param tag  The tag the attributes are for
     @param keys List of valid attributes for the tag
     @return this (for chaining)
     */
    public Whitelist addAttributes(String tag, String... keys) {
        Validate.notEmpty(tag);
        Validate.notNull(keys);

        TagName tagName = TagName.valueOf(tag);
        Set<AttributeKey> attributeSet = new HashSet<AttributeKey>();
        for (String key : keys) {
            Validate.notEmpty(key);
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

    /**
     Add an enforced attribute to a tag. An enforced attribute will always be added to the element. If the element
     already has the attribute set, it will be overridden.
     <p/>
     E.g.: <code>addEnforcedAttribute("a", "rel", "nofollow")</code> will make all <code>a</code> tags output as
     <code>&lt;a href="..." rel="nofollow"></code>

     @param tag   The tag the enforced attribute is for
     @param key   The attribute key
     @param value The enforced attribute value
     @return this (for chaining)
     */
    public Whitelist addEnforcedAttribute(String tag, String key, String value) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notEmpty(value);

        TagName tagName = TagName.valueOf(tag);
        AttributeKey attrKey = AttributeKey.valueOf(key);
        AttributeValue attrVal = AttributeValue.valueOf(value);

        if (enforcedAttributes.containsKey(tagName)) {
            enforcedAttributes.get(tagName).put(attrKey, attrVal);
        } else {
            Map<AttributeKey, AttributeValue> attrMap = new HashMap<AttributeKey, AttributeValue>();
            attrMap.put(attrKey, attrVal);
            enforcedAttributes.put(tagName, attrMap);
        }
        return this;
    }

    /**
     Add allowed URL protocols for an element's URL attribute. This restricts the possible values of the attribute to
     URLs with the defined protocol.
     <p/>
     E.g.: <code>addProtocols("a", "href", "ftp", "http", "https")</code>

     @param tag       Tag the URL protocol is for
     @param key       Attribute key
     @param protocols List of valid protocols
     @return this, for chaining
     */
    public Whitelist addProtocols(String tag, String key, String... protocols) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notNull(protocols);

        TagName tagName = TagName.valueOf(tag);
        AttributeKey attrKey = AttributeKey.valueOf(key);
        Map<AttributeKey, Set<Protocol>> attrMap;
        Set<Protocol> protSet;

        if (this.protocols.containsKey(tagName)) {
            attrMap = this.protocols.get(tagName);
        } else {
            attrMap = new HashMap<AttributeKey, Set<Protocol>>();
            this.protocols.put(tagName, attrMap);
        }
        if (attrMap.containsKey(attrKey)) {
            protSet = attrMap.get(attrKey);
        } else {
            protSet = new HashSet<Protocol>();
            attrMap.put(attrKey, protSet);
        }
        for (String protocol : protocols) {
            Validate.notEmpty(protocol);
            Protocol prot = Protocol.valueOf(protocol);
            protSet.add(prot);
        }
        return this;
    }

    boolean isSafeTag(String tag) {
        return tagNames.contains(TagName.valueOf(tag));
    }

    boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
        TagName tag = TagName.valueOf(tagName);
        AttributeKey key = AttributeKey.valueOf(attr.getKey());

        if (attributes.containsKey(tag)) {
            if (attributes.get(tag).contains(key)) {
                if (protocols.containsKey(tag)) {
                    Map<AttributeKey, Set<Protocol>> attrProts = protocols.get(tag);
                    // ok if not defined protocol; otherwise test
                    return !attrProts.containsKey(key) || testValidProtocol(el, attr, attrProts.get(key));
                } else { // attribute found, no protocols defined, so OK
                    return true;
                }
            }
        } else { // no attributes defined for tag, try :all tag
            return !tagName.equals(":all") && isSafeAttribute(":all", el, attr);
        }
        return false;
    }

    private boolean testValidProtocol(Element el, Attribute attr, Set<Protocol> protocols) {
        // resolve relative urls to abs, and update the attribute so output html has abs.
        // rels without a baseuri get removed
        String value = el.absUrl(attr.getKey());
        attr.setValue(value);
        
        for (Protocol protocol : protocols) {
            String prot = protocol.toString() + ":";
            if (value.toLowerCase().startsWith(prot)) {
                return true;
            }
        }
        return false;
    }

    Attributes getEnforcedAttributes(String tagName) {
        Attributes attrs = new Attributes();
        TagName tag = TagName.valueOf(tagName);
        if (enforcedAttributes.containsKey(tag)) {
            Map<AttributeKey, AttributeValue> keyVals = enforcedAttributes.get(tag);
            for (Map.Entry<AttributeKey, AttributeValue> entry : keyVals.entrySet()) {
                attrs.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return attrs;
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
            Validate.notNull(value);
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

