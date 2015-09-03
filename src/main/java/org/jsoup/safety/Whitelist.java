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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 Whitelists define what HTML (elements and attributes) to allow through the cleaner. Everything else is removed.
 <p>
 Start with one of the defaults:
 </p>
 <ul>
 <li>{@link #none}
 <li>{@link #simpleText}
 <li>{@link #basic}
 <li>{@link #basicWithImages}
 <li>{@link #relaxed}
 </ul>
 <p>
 If you need to allow more through (please be careful!), tweak a base whitelist with:
 </p>
 <ul>
 <li>{@link #addTags}
 <li>{@link #addAttributes}
 <li>{@link #addEnforcedAttribute}
 <li>{@link #addProtocols}
 </ul>
 <p>
 You can remove any setting from an existing whitelist with:
 </p>
 <ul>
 <li>{@link #removeTags}
 <li>{@link #removeAttributes}
 <li>{@link #removeEnforcedAttribute}
 <li>{@link #removeProtocols}
 </ul>
 
 <p>
 The cleaner and these whitelists assume that you want to clean a <code>body</code> fragment of HTML (to add user
 supplied HTML into a templated page), and not to clean a full HTML document. If the latter is the case, either wrap the
 document HTML around the cleaned body HTML, or create a whitelist that allows <code>html</code> and <code>head</code>
 elements as appropriate.
 </p>
 <p>
 If you are going to extend a whitelist, please be very careful. Make sure you understand what attributes may lead to
 XSS attack vectors. URL attributes are particularly vulnerable and require careful validation. See 
 http://ha.ckers.org/xss.html for some XSS attack examples.
 </p>

 @author Jonathan Hedley
 */
public class Whitelist {
    private Set<TagName> tagNames; // tags allowed, lower case. e.g. [p, br, span]
    private Map<TagName, Map<AttributeKey, Set<ValueValidator>>> attributes; // allowed attributes for tag with optional validators[]
    private Map<TagName, Map<AttributeKey, AttributeValue>> enforcedAttributes; // always set these attribute values
    private boolean preserveRelativeLinks; // option to preserve relative links

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
     <p>
     This whitelist allows a fuller range of text nodes: <code>a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li,
     ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul</code>, and appropriate attributes.
     </p>
     <p>
     Links (<code>a</code> elements) can point to <code>http, https, ftp, mailto</code>, and have an enforced
     <code>rel=nofollow</code> attribute.
     </p>
     <p>
     Does not allow images.
     </p>

     @return whitelist
     */
    public static Whitelist basic() {
        return new Whitelist()
                .addTags(
                        "a", "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
                        "i", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong", "sub",
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
     code, col, colgroup, dd, div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, small, span, strike, strong, sub,
     sup, table, tbody, td, tfoot, th, thead, tr, u, ul</code>
     <p>
     Links do not have an enforced <code>rel=nofollow</code> attribute, but you can add that if desired.
     </p>

     @return whitelist
     */
    public static Whitelist relaxed() {
        return new Whitelist()
                .addTags(
                        "a", "b", "blockquote", "br", "caption", "cite", "code", "col",
                        "colgroup", "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                        "i", "img", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong",
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
                .addProtocols("cite", "cite", "http", "https")
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
        attributes = new HashMap<TagName, Map<AttributeKey, Set<ValueValidator>>>();
        enforcedAttributes = new HashMap<TagName, Map<AttributeKey, AttributeValue>>();
        preserveRelativeLinks = false;
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
     Remove a list of allowed elements from a whitelist. (If a tag is not allowed, it will be removed from the HTML.)

     @param tags tag names to disallow
     @return this (for chaining)
     */
    public Whitelist removeTags(String... tags) {
        Validate.notNull(tags);

        for(String tag: tags) {
            Validate.notEmpty(tag);
            TagName tagName = TagName.valueOf(tag);

            if(tagNames.remove(tagName)) { // Only look in sub-maps if tag was allowed
                attributes.remove(tagName);
                enforcedAttributes.remove(tagName);
            }
        }
        return this;
    }

    /**
     Add a list of allowed attributes to a tag. (If an attribute is not allowed on an element, it will be removed.)
     <p>
     E.g.: <code>addAttributes("a", "href", "class")</code> allows <code>href</code> and <code>class</code> attributes
     on <code>a</code> tags.
     </p>
     <p>
     To make an attribute valid for <b>all tags</b>, use the pseudo tag <code>:all</code>, e.g.
     <code>addAttributes(":all", "class")</code>.
     </p>

     @param tag  The tag the attributes are for. The tag will be added to the allowed tag list if necessary.
     @param keys List of valid attributes for the tag
     @return this (for chaining)
     */
    public Whitelist addAttributes(String tag, String... keys) {
        Validate.notEmpty(tag);
        Validate.notNull(keys);
        Validate.isTrue(keys.length > 0, "No attributes supplied.");

        TagName tagName = TagName.valueOf(tag);
        if (!tagNames.contains(tagName))
            tagNames.add(tagName);
        Set<AttributeKey> attributeSet = new HashSet<AttributeKey>();
        for (String key : keys) {
            Validate.notEmpty(key);
            attributeSet.add(AttributeKey.valueOf(key));
        }
        
        final Map<AttributeKey, Set<ValueValidator>> currentSet;
        if (attributes.containsKey(tagName)) {
            currentSet = attributes.get(tagName);
        } else {
            currentSet = new HashMap<Whitelist.AttributeKey, Set<ValueValidator>>();
            attributes.put(tagName, currentSet);
        }
        for(AttributeKey attribute: attributeSet) {
            currentSet.put(attribute, new HashSet<ValueValidator>());
        }
        return this;
    }

    /**
    Add a validator for an attribute. This allows the value of an attribute to be verified using one of a number of
    built-in validators, or developers can implement their own by implementing {@link ValueValidator} or extending
    {@link BaseValueValidator}. Generally an attribute is safe if it passes at least one validator but validators can be
    marked as required in which case attributes must match all required validators.
    <p>
    E.g.: <code>addAttributeValidator("p", "class", new PatternValueValidator("^x-.*$"))</code> will ensure that
    <code>p</code> tags only specify <code>class</code> attribute values that begin with "x-"
    </p>

    @param tag   The tag the attribute validator is for. The tag will be added to the allowed tag list if necessary.
    @param key   The attribute key. The attribute will be added to the allowed list if necessary.
    @param validator The attribute validator
    @return this (for chaining)
    */
    public Whitelist addAttributeValidator(String tag, String key, ValueValidator validator) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notNull(validator);

        TagName tagName = TagName.valueOf(tag);
        if (!tagNames.contains(tagName))
            tagNames.add(tagName);

        final Map<AttributeKey, Set<ValueValidator>> currentSet;
        if (attributes.containsKey(tagName)) {
            currentSet = attributes.get(tagName);
        } else {
            currentSet = new HashMap<Whitelist.AttributeKey, Set<ValueValidator>>();
            attributes.put(tagName, currentSet);
        }

        AttributeKey attribute = AttributeKey.valueOf(key);
        final Set<ValueValidator> validators;
        if (currentSet.containsKey(attribute)) {
            validators = currentSet.get(attribute);
        } else {
            validators = new HashSet<ValueValidator>();
            currentSet.put(attribute, validators);
        }
        validators.add(validator);

        return this;
    }

    /**
     Remove a list of allowed attributes from a tag. (If an attribute is not allowed on an element, it will be removed.)
     <p>
     E.g.: <code>removeAttributes("a", "href", "class")</code> disallows <code>href</code> and <code>class</code>
     attributes on <code>a</code> tags.
     </p>
     <p>
     To make an attribute invalid for <b>all tags</b>, use the pseudo tag <code>:all</code>, e.g.
     <code>removeAttributes(":all", "class")</code>.
     </p>

     @param tag  The tag the attributes are for.
     @param keys List of invalid attributes for the tag
     @return this (for chaining)
     */
    public Whitelist removeAttributes(String tag, String... keys) {
        Validate.notEmpty(tag);
        Validate.notNull(keys);
        Validate.isTrue(keys.length > 0, "No attributes supplied.");

        TagName tagName = TagName.valueOf(tag);
        Set<AttributeKey> attributeSet = new HashSet<AttributeKey>();
        for (String key : keys) {
            Validate.notEmpty(key);
            attributeSet.add(AttributeKey.valueOf(key));
        }
        if(tagNames.contains(tagName) && attributes.containsKey(tagName)) { // Only look in sub-maps if tag was allowed
            Map<AttributeKey, Set<ValueValidator>> currentSet = attributes.get(tagName);
            for(AttributeKey attribute: attributeSet) {
                currentSet.remove(attribute);
            }

            if(currentSet.isEmpty()) // Remove tag from attribute map if no attributes are allowed for tag
                attributes.remove(tagName);
        }
        if(tag.equals(":all")) // Attribute needs to be removed from all individually set tags
            for(TagName name: attributes.keySet()) {
                Map<AttributeKey, Set<ValueValidator>> currentSet = attributes.get(name);
                for(AttributeKey attribute: attributeSet) {
                    currentSet.remove(attribute);
                }

                if(currentSet.isEmpty()) // Remove tag from attribute map if no attributes are allowed for tag
                    attributes.remove(name);
            }
        return this;
    }

    /**
     Add an enforced attribute to a tag. An enforced attribute will always be added to the element. If the element
     already has the attribute set, it will be overridden.
     <p>
     E.g.: <code>addEnforcedAttribute("a", "rel", "nofollow")</code> will make all <code>a</code> tags output as
     <code>&lt;a href="..." rel="nofollow"&gt;</code>
     </p>

     @param tag   The tag the enforced attribute is for. The tag will be added to the allowed tag list if necessary.
     @param key   The attribute key
     @param value The enforced attribute value
     @return this (for chaining)
     */
    public Whitelist addEnforcedAttribute(String tag, String key, String value) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notEmpty(value);

        TagName tagName = TagName.valueOf(tag);
        if (!tagNames.contains(tagName))
            tagNames.add(tagName);
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
     Remove a previously configured enforced attribute from a tag.

     @param tag   The tag the enforced attribute is for.
     @param key   The attribute key
     @return this (for chaining)
     */
    public Whitelist removeEnforcedAttribute(String tag, String key) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);

        TagName tagName = TagName.valueOf(tag);
        if(tagNames.contains(tagName) && enforcedAttributes.containsKey(tagName)) {
            AttributeKey attrKey = AttributeKey.valueOf(key);
            Map<AttributeKey, AttributeValue> attrMap = enforcedAttributes.get(tagName);
            attrMap.remove(attrKey);

            if(attrMap.isEmpty()) // Remove tag from enforced attribute map if no enforced attributes are present
                enforcedAttributes.remove(tagName);
        }
        return this;
    }

    /**
     * Configure this Whitelist to preserve relative links in an element's URL attribute, or convert them to absolute
     * links. By default, this is <b>false</b>: URLs will be  made absolute (e.g. start with an allowed protocol, like
     * e.g. {@code http://}.
     * <p>
     * Note that when handling relative links, the input document must have an appropriate {@code base URI} set when
     * parsing, so that the link's protocol can be confirmed. Regardless of the setting of the {@code preserve relative
     * links} option, the link must be resolvable against the base URI to an allowed protocol; otherwise the attribute
     * will be removed.
     * </p>
     *
     * @param preserve {@code true} to allow relative links, {@code false} (default) to deny
     * @return this Whitelist, for chaining.
     * @see #addProtocols
     */
    public Whitelist preserveRelativeLinks(boolean preserve) {
        preserveRelativeLinks = preserve;
        return this;
    }

    /**
     Add allowed URL protocols for an element's URL attribute. This restricts the possible values of the attribute to
     URLs with the defined protocol.
     <p>
     E.g.: <code>addProtocols("a", "href", "ftp", "http", "https")</code>
     </p>
     <p>
     To allow a link to an in-page URL anchor (i.e. <code>&lt;a href="#anchor"&gt;</code>, add a <code>#</code>:<br>
     E.g.: <code>addProtocols("a", "href", "#")</code>
     </p>

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

        if (this.attributes.containsKey(tagName)) {
            Map<AttributeKey, Set<ValueValidator>> attrMap = this.attributes.get(tagName);
            if (attrMap.containsKey(attrKey)) {
                ProtocolValidator protocolValidator = null;
                Set<ValueValidator> validators = attrMap.get(attrKey);
                for (ValueValidator validator : validators) {
                    if (validator instanceof ProtocolValidator) {
                        protocolValidator = (ProtocolValidator) validator;
                        break;
                    }
                }
                if (protocolValidator == null) {
                    protocolValidator = new ProtocolValidator();
                    validators.add(protocolValidator);
                }
                protocolValidator.addProtocols(protocols);
            }
        }
        return this;
    }

    /**
     Remove allowed URL protocols for an element's URL attribute.
     <p>
     E.g.: <code>removeProtocols("a", "href", "ftp")</code>
     </p>

     @param tag       Tag the URL protocol is for
     @param key       Attribute key
     @param protocols List of invalid protocols
     @return this, for chaining
     */
    public Whitelist removeProtocols(String tag, String key, String... protocols) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notNull(protocols);

        TagName tagName = TagName.valueOf(tag);
        AttributeKey attrKey = AttributeKey.valueOf(key);

        if (this.attributes.containsKey(tagName)) {
            Map<AttributeKey, Set<ValueValidator>> attrMap = this.attributes.get(tagName);
            if(attrMap.containsKey(attrKey)) {
                Set<ValueValidator> attrValidators = attrMap.get(attrKey);
                Iterator<ValueValidator> iter = attrValidators.iterator();
                while (iter.hasNext()) {
                    ValueValidator validator = iter.next();
                    if (validator instanceof ProtocolValidator) {
                        ProtocolValidator protocolValidator = (ProtocolValidator) validator;
                        protocolValidator.removeProtocols(protocols);
                        if (protocolValidator.isEmpty()) {
                            iter.remove();
                        }
                        break;
                    }
                }
            }
        }
        return this;
    }

    /**
     * Test if the supplied tag is allowed by this whitelist
     * @param tag test tag
     * @return true if allowed
     */
    protected boolean isSafeTag(String tag) {
        return tagNames.contains(TagName.valueOf(tag));
    }

    /**
     * Test if the supplied attribute is allowed by this whitelist for this tag
     * @param tagName tag to consider allowing the attribute in
     * @param el element under test, to confirm protocol
     * @param attr attribute under test
     * @return true if allowed
     */
    protected boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
        TagName tag = TagName.valueOf(tagName);
        AttributeKey key = AttributeKey.valueOf(attr.getKey());

        if (attributes.containsKey(tag)) {
            Map<AttributeKey, Set<ValueValidator>> tagAttributes = attributes.get(tag);
            if (tagAttributes.containsKey(key)) {
                Set<ValueValidator> validators = tagAttributes.get(key);
                if (validators.isEmpty()) {
                    return true;
                }

                boolean safe = false;
                for (ValueValidator validator : validators) {
                    boolean accept = validator.isSafe(el, attr);
                    if (!accept && validator.isRequired()) {
                        return false;
                    }

                    safe |= accept;
                }
                return safe;
            }
        }
        // no attributes defined for tag, try :all tag
        return !tagName.equals(":all") && isSafeAttribute(":all", el, attr);
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

    final class ProtocolValidator implements ValueValidator {

        private final Set<Protocol> protocols = new HashSet<Protocol>();

        public boolean isRequired() {
            return true;
        }

        public boolean isSafe(Element el, Attribute attr) {
            // try to resolve relative urls to abs, and optionally update the attribute so output html has abs.
            // rels without a baseuri get removed
            String value = el.absUrl(attr.getKey());
            if (value.length() == 0)
                value = attr.getValue(); // if it could not be made abs, run as-is to allow custom unknown protocols
            if (!preserveRelativeLinks)
                attr.setValue(value);

            for (Protocol protocol : protocols) {
                String prot = protocol.toString();

                if (prot.equals("#")) { // allows anchor links
                    if (isValidAnchor(value)) {
                        return true;
                    } else {
                        continue;
                    }
                }

                prot += ":";

                if (value.toLowerCase().startsWith(prot)) {
                    return true;
                }
            }
            return false;
        }

        public void addProtocols(String... protocols) {
            for (String protocol : protocols) {
                Validate.notEmpty(protocol);
                Protocol prot = Protocol.valueOf(protocol);
                this.protocols.add(prot);
            }
        }

        private void removeProtocols(String... protocols) {
            for (String protocol : protocols) {
                Validate.notEmpty(protocol);
                this.protocols.remove(Protocol.valueOf(protocol));
            }
        }

        private boolean isEmpty() {
            return protocols.isEmpty();
        }

        private boolean isValidAnchor(String value) {
            return value.startsWith("#") && !value.matches(".*\\s.*");
        }
    }
}

