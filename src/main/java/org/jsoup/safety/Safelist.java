package org.jsoup.safety;

/*
    Thank you to Ryan Grove (wonko.com) for the Ruby HTML cleaner http://github.com/rgrove/sanitize/, which inspired
    this safe-list configuration, and the initial defaults.
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

import static org.jsoup.internal.Normalizer.lowerCase;


/**
 Safe-lists define what HTML (elements and attributes) to allow through the cleaner. Everything else is removed.
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
 If you need to allow more through (please be careful!), tweak a base safelist with:
 </p>
 <ul>
 <li>{@link #addTags(String... tagNames)}
 <li>{@link #addAttributes(String tagName, String... attributes)}
 <li>{@link #addEnforcedAttribute(String tagName, String attribute, String value)}
 <li>{@link #addProtocols(String tagName, String attribute, String... protocols)}
 </ul>
 <p>
 You can remove any setting from an existing safelist with:
 </p>
 <ul>
 <li>{@link #removeTags(String... tagNames)}
 <li>{@link #removeAttributes(String tagName, String... attributes)}
 <li>{@link #removeEnforcedAttribute(String tagName, String attribute)}
 <li>{@link #removeProtocols(String tagName, String attribute, String... removeProtocols)}
 </ul>

 <p>
 The cleaner and these safelists assume that you want to clean a <code>body</code> fragment of HTML (to add user
 supplied HTML into a templated page), and not to clean a full HTML document. If the latter is the case, either wrap the
 document HTML around the cleaned body HTML, or create a safelist that allows <code>html</code> and <code>head</code>
 elements as appropriate.
 </p>
 <p>
 If you are going to extend a safelist, please be very careful. Make sure you understand what attributes may lead to
 XSS attack vectors. URL attributes are particularly vulnerable and require careful validation. See 
 the <a href="https://owasp.org/www-community/xss-filter-evasion-cheatsheet">XSS Filter Evasion Cheat Sheet</a> for some
 XSS attack examples (that jsoup will safegaurd against the default Cleaner and Safelist configuration).
 </p>
 */
public class Safelist {
    private static final String All = ":all";
    private final Set<TagName> tagNames; // tags allowed, lower case. e.g. [p, br, span]
    private final Map<TagName, Set<AttributeKey>> attributes; // tag -> attribute[]. allowed attributes [href] for a tag.
    private final Map<TagName, Map<AttributeKey, AttributeValue>> enforcedAttributes; // always set these attribute values
    private final Map<TagName, Map<AttributeKey, Set<Protocol>>> protocols; // allowed URL protocols for attributes
    private boolean preserveRelativeLinks; // option to preserve relative links

    /**
     This safelist allows only text nodes: any HTML Element or any Node other than a TextNode will be removed.
     <p>
     Note that the output of {@link org.jsoup.Jsoup#clean(String, Safelist)} is still <b>HTML</b> even when using
     this Safelist, and so any HTML entities in the output will be appropriately escaped. If you want plain text, not
     HTML, you should use a text method such as {@link Element#text()} instead, after cleaning the document.
     </p>
     <p>Example:</p>
     <pre>{@code
     String sourceBodyHtml = "<p>5 is &lt; 6.</p>";
     String html = Jsoup.clean(sourceBodyHtml, Safelist.none());

     Cleaner cleaner = new Cleaner(Safelist.none());
     String text = cleaner.clean(Jsoup.parse(sourceBodyHtml)).text();

     // html is: 5 is &lt; 6.
     // text is: 5 is < 6.
     }</pre>

     @return safelist
     */
    public static Safelist none() {
        return new Safelist();
    }

    /**
     This safelist allows only simple text formatting: <code>b, em, i, strong, u</code>. All other HTML (tags and
     attributes) will be removed.

     @return safelist
     */
    public static Safelist simpleText() {
        return new Safelist()
                .addTags("b", "em", "i", "strong", "u")
                ;
    }

    /**
     <p>
     This safelist allows a fuller range of text nodes: <code>a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li,
     ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul</code>, and appropriate attributes.
     </p>
     <p>
     Links (<code>a</code> elements) can point to <code>http, https, ftp, mailto</code>, and have an enforced
     <code>rel=nofollow</code> attribute.
     </p>
     <p>
     Does not allow images.
     </p>

     @return safelist
     */
    public static Safelist basic() {
        return new Safelist()
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
     This safelist allows the same text tags as {@link #basic}, and also allows <code>img</code> tags, with appropriate
     attributes, with <code>src</code> pointing to <code>http</code> or <code>https</code>.

     @return safelist
     */
    public static Safelist basicWithImages() {
        return basic()
                .addTags("img")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addProtocols("img", "src", "http", "https")
                ;
    }

    /**
     This safelist allows a full range of text and structural body HTML: <code>a, b, blockquote, br, caption, cite,
     code, col, colgroup, dd, div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, small, span, strike, strong, sub,
     sup, table, tbody, td, tfoot, th, thead, tr, u, ul</code>
     <p>
     Links do not have an enforced <code>rel=nofollow</code> attribute, but you can add that if desired.
     </p>

     @return safelist
     */
    public static Safelist relaxed() {
        return new Safelist()
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
     Create a new, empty safelist. Generally it will be better to start with a default prepared safelist instead.

     @see #basic()
     @see #basicWithImages()
     @see #simpleText()
     @see #relaxed()
     */
    public Safelist() {
        tagNames = new HashSet<>();
        attributes = new HashMap<>();
        enforcedAttributes = new HashMap<>();
        protocols = new HashMap<>();
        preserveRelativeLinks = false;
    }

    /**
     Deep copy an existing Safelist to a new Safelist.
     @param copy the Safelist to copy
     */
    public Safelist(Safelist copy) {
        this();
        tagNames.addAll(copy.tagNames);
        for (Map.Entry<TagName, Set<AttributeKey>> copyTagAttributes : copy.attributes.entrySet()) {
            attributes.put(copyTagAttributes.getKey(), new HashSet<>(copyTagAttributes.getValue()));
        }
        for (Map.Entry<TagName, Map<AttributeKey, AttributeValue>> enforcedEntry : copy.enforcedAttributes.entrySet()) {
            enforcedAttributes.put(enforcedEntry.getKey(), new HashMap<>(enforcedEntry.getValue()));
        }
        for (Map.Entry<TagName, Map<AttributeKey, Set<Protocol>>> protocolsEntry : copy.protocols.entrySet()) {
            Map<AttributeKey, Set<Protocol>> attributeProtocolsCopy = new HashMap<>();
            for (Map.Entry<AttributeKey, Set<Protocol>> attributeProtocols : protocolsEntry.getValue().entrySet()) {
                attributeProtocolsCopy.put(attributeProtocols.getKey(), new HashSet<>(attributeProtocols.getValue()));
            }
            protocols.put(protocolsEntry.getKey(), attributeProtocolsCopy);
        }
        preserveRelativeLinks = copy.preserveRelativeLinks;
    }

    /**
     Add a list of allowed elements to a safelist. (If a tag is not allowed, it will be removed from the HTML.)

     @param tags tag names to allow
     @return this (for chaining)
     */
    public Safelist addTags(String... tags) {
        Validate.notNull(tags);

        for (String tagName : tags) {
            Validate.notEmpty(tagName);
            tagNames.add(TagName.valueOf(tagName));
        }
        return this;
    }

    /**
     Remove a list of allowed elements from a safelist. (If a tag is not allowed, it will be removed from the HTML.)

     @param tags tag names to disallow
     @return this (for chaining)
     */
    public Safelist removeTags(String... tags) {
        Validate.notNull(tags);

        for(String tag: tags) {
            Validate.notEmpty(tag);
            TagName tagName = TagName.valueOf(tag);

            if(tagNames.remove(tagName)) { // Only look in sub-maps if tag was allowed
                attributes.remove(tagName);
                enforcedAttributes.remove(tagName);
                protocols.remove(tagName);
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
     @param attributes List of valid attributes for the tag
     @return this (for chaining)
     */
    public Safelist addAttributes(String tag, String... attributes) {
        Validate.notEmpty(tag);
        Validate.notNull(attributes);
        Validate.isTrue(attributes.length > 0, "No attribute names supplied.");

        TagName tagName = TagName.valueOf(tag);
        tagNames.add(tagName);
        Set<AttributeKey> attributeSet = new HashSet<>();
        for (String key : attributes) {
            Validate.notEmpty(key);
            attributeSet.add(AttributeKey.valueOf(key));
        }
        if (this.attributes.containsKey(tagName)) {
            Set<AttributeKey> currentSet = this.attributes.get(tagName);
            currentSet.addAll(attributeSet);
        } else {
            this.attributes.put(tagName, attributeSet);
        }
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
     @param attributes List of invalid attributes for the tag
     @return this (for chaining)
     */
    public Safelist removeAttributes(String tag, String... attributes) {
        Validate.notEmpty(tag);
        Validate.notNull(attributes);
        Validate.isTrue(attributes.length > 0, "No attribute names supplied.");

        TagName tagName = TagName.valueOf(tag);
        Set<AttributeKey> attributeSet = new HashSet<>();
        for (String key : attributes) {
            Validate.notEmpty(key);
            attributeSet.add(AttributeKey.valueOf(key));
        }
        if(tagNames.contains(tagName) && this.attributes.containsKey(tagName)) { // Only look in sub-maps if tag was allowed
            Set<AttributeKey> currentSet = this.attributes.get(tagName);
            currentSet.removeAll(attributeSet);

            if(currentSet.isEmpty()) // Remove tag from attribute map if no attributes are allowed for tag
                this.attributes.remove(tagName);
        }
        if(tag.equals(All)) { // Attribute needs to be removed from all individually set tags
            Iterator<Map.Entry<TagName, Set<AttributeKey>>> it = this.attributes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<TagName, Set<AttributeKey>> entry = it.next();
                Set<AttributeKey> currentSet = entry.getValue();
                currentSet.removeAll(attributeSet);
                if(currentSet.isEmpty()) // Remove tag from attribute map if no attributes are allowed for tag
                    it.remove();
            }
        }
        return this;
    }

    /**
     Add an enforced attribute to a tag. An enforced attribute will always be added to the element. If the element
     already has the attribute set, it will be overridden with this value.
     <p>
     E.g.: <code>addEnforcedAttribute("a", "rel", "nofollow")</code> will make all <code>a</code> tags output as
     <code>&lt;a href="..." rel="nofollow"&gt;</code>
     </p>

     @param tag   The tag the enforced attribute is for. The tag will be added to the allowed tag list if necessary.
     @param attribute   The attribute name
     @param value The enforced attribute value
     @return this (for chaining)
     */
    public Safelist addEnforcedAttribute(String tag, String attribute, String value) {
        Validate.notEmpty(tag);
        Validate.notEmpty(attribute);
        Validate.notEmpty(value);

        TagName tagName = TagName.valueOf(tag);
        tagNames.add(tagName);
        AttributeKey attrKey = AttributeKey.valueOf(attribute);
        AttributeValue attrVal = AttributeValue.valueOf(value);

        if (enforcedAttributes.containsKey(tagName)) {
            enforcedAttributes.get(tagName).put(attrKey, attrVal);
        } else {
            Map<AttributeKey, AttributeValue> attrMap = new HashMap<>();
            attrMap.put(attrKey, attrVal);
            enforcedAttributes.put(tagName, attrMap);
        }
        return this;
    }

    /**
     Remove a previously configured enforced attribute from a tag.

     @param tag   The tag the enforced attribute is for.
     @param attribute   The attribute name
     @return this (for chaining)
     */
    public Safelist removeEnforcedAttribute(String tag, String attribute) {
        Validate.notEmpty(tag);
        Validate.notEmpty(attribute);

        TagName tagName = TagName.valueOf(tag);
        if(tagNames.contains(tagName) && enforcedAttributes.containsKey(tagName)) {
            AttributeKey attrKey = AttributeKey.valueOf(attribute);
            Map<AttributeKey, AttributeValue> attrMap = enforcedAttributes.get(tagName);
            attrMap.remove(attrKey);

            if(attrMap.isEmpty()) // Remove tag from enforced attribute map if no enforced attributes are present
                enforcedAttributes.remove(tagName);
        }
        return this;
    }

    /**
     * Configure this Safelist to preserve relative links in an element's URL attribute, or convert them to absolute
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
     * @return this Safelist, for chaining.
     * @see #addProtocols
     */
    public Safelist preserveRelativeLinks(boolean preserve) {
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
     @param attribute       Attribute name
     @param protocols List of valid protocols
     @return this, for chaining
     */
    public Safelist addProtocols(String tag, String attribute, String... protocols) {
        Validate.notEmpty(tag);
        Validate.notEmpty(attribute);
        Validate.notNull(protocols);

        TagName tagName = TagName.valueOf(tag);
        AttributeKey attrKey = AttributeKey.valueOf(attribute);
        Map<AttributeKey, Set<Protocol>> attrMap;
        Set<Protocol> protSet;

        if (this.protocols.containsKey(tagName)) {
            attrMap = this.protocols.get(tagName);
        } else {
            attrMap = new HashMap<>();
            this.protocols.put(tagName, attrMap);
        }
        if (attrMap.containsKey(attrKey)) {
            protSet = attrMap.get(attrKey);
        } else {
            protSet = new HashSet<>();
            attrMap.put(attrKey, protSet);
        }
        for (String protocol : protocols) {
            Validate.notEmpty(protocol);
            Protocol prot = Protocol.valueOf(protocol);
            protSet.add(prot);
        }
        return this;
    }

    /**
     Remove allowed URL protocols for an element's URL attribute. If you remove all protocols for an attribute, that
     attribute will allow any protocol.
     <p>
     E.g.: <code>removeProtocols("a", "href", "ftp")</code>
     </p>

     @param tag Tag the URL protocol is for
     @param attribute Attribute name
     @param removeProtocols List of invalid protocols
     @return this, for chaining
     */
    public Safelist removeProtocols(String tag, String attribute, String... removeProtocols) {
        Validate.notEmpty(tag);
        Validate.notEmpty(attribute);
        Validate.notNull(removeProtocols);

        TagName tagName = TagName.valueOf(tag);
        AttributeKey attr = AttributeKey.valueOf(attribute);

        // make sure that what we're removing actually exists; otherwise can open the tag to any data and that can
        // be surprising
        Validate.isTrue(protocols.containsKey(tagName), "Cannot remove a protocol that is not set.");
        Map<AttributeKey, Set<Protocol>> tagProtocols = protocols.get(tagName);
        Validate.isTrue(tagProtocols.containsKey(attr), "Cannot remove a protocol that is not set.");

        Set<Protocol> attrProtocols = tagProtocols.get(attr);
        for (String protocol : removeProtocols) {
            Validate.notEmpty(protocol);
            attrProtocols.remove(Protocol.valueOf(protocol));
        }

        if (attrProtocols.isEmpty()) { // Remove protocol set if empty
            tagProtocols.remove(attr);
            if (tagProtocols.isEmpty()) // Remove entry for tag if empty
                protocols.remove(tagName);
        }
        return this;
    }

    /**
     * Test if the supplied tag is allowed by this safelist
     * @param tag test tag
     * @return true if allowed
     */
    protected boolean isSafeTag(String tag) {
        return tagNames.contains(TagName.valueOf(tag));
    }

    /**
     * Test if the supplied attribute is allowed by this safelist for this tag
     * @param tagName tag to consider allowing the attribute in
     * @param el element under test, to confirm protocol
     * @param attr attribute under test
     * @return true if allowed
     */
    protected boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
        TagName tag = TagName.valueOf(tagName);
        AttributeKey key = AttributeKey.valueOf(attr.getKey());

        Set<AttributeKey> okSet = attributes.get(tag);
        if (okSet != null && okSet.contains(key)) {
            if (protocols.containsKey(tag)) {
                Map<AttributeKey, Set<Protocol>> attrProts = protocols.get(tag);
                // ok if not defined protocol; otherwise test
                return !attrProts.containsKey(key) || testValidProtocol(el, attr, attrProts.get(key));
            } else { // attribute found, no protocols defined, so OK
                return true;
            }
        }
        // might be an enforced attribute?
        Map<AttributeKey, AttributeValue> enforcedSet = enforcedAttributes.get(tag);
        if (enforcedSet != null) {
            Attributes expect = getEnforcedAttributes(tagName);
            String attrKey = attr.getKey();
            if (expect.hasKeyIgnoreCase(attrKey)) {
                return expect.getIgnoreCase(attrKey).equals(attr.getValue());
            }
        }
        // no attributes defined for tag, try :all tag
        return !tagName.equals(All) && isSafeAttribute(All, el, attr);
    }

    private boolean testValidProtocol(Element el, Attribute attr, Set<Protocol> protocols) {
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

            if (lowerCase(value).startsWith(prot)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidAnchor(String value) {
        return value.startsWith("#") && !value.matches(".*\\s.*");
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
        private final String value;

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
                return other.value == null;
            } else return value.equals(other.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
