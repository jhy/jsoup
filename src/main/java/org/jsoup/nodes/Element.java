package org.jsoup.nodes;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.StringUtils;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Collector;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;

import java.util.*;

/**
 * A HTML element consists of a tag name, attributes, and child nodes (including text nodes and
 * other elements).
 * 
 * From an Element, you can extract data, traverse the node graph, and manipulate the HTML.
 * 
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class Element extends Node {
    private final Tag tag;
    private Set<String> classNames;
    
    /**
     * Create a new, standalone Element. (Standalone in that is has no parent.)
     * 
     * @param tag tag of this element
     * @param baseUri the base URI
     * @param attributes initial attributes
     * @see #appendChild(Node)
     * @see #appendElement(String)
     */
    public Element(Tag tag, String baseUri, Attributes attributes) {
        super(baseUri, attributes);
        
        Validate.notNull(tag);    
        this.tag = tag;
    }
    
    /**
     * Create a new Element from a tag and a base URI.
     * 
     * @param tag element tag
     * @param baseUri the base URI of this element. It is acceptable for the base URI to be an empty
     *            string, but not null.
     * @see Tag#valueOf(String)
     */
    public Element(Tag tag, String baseUri) {
        this(tag, baseUri, new Attributes());
    }

    @Override
    public String nodeName() {
        return tag.getName();
    }

    /**
     * Get the name of the tag for this element. E.g. {@code div}
     * 
     * @return the tag name
     */
    public String tagName() {
        return tag.getName();
    }

    /**
     * Get the Tag for this element.
     * 
     * @return the tag object
     */
    public Tag tag() {
        return tag;
    }
    
    /**
     * Test if this element is a block-level element. (E.g. {@code <div> == true} or an inline element
     * {@code <p> == false}).
     * 
     * @return true if block, false if not (and thus inline)
     */
    public boolean isBlock() {
        return tag.isBlock();
    }

    /**
     * Get the {@code id} attribute of this element.
     * 
     * @return The id attribute, if present, or an empty string if not.
     */
    public String id() {
        String id = attr("id");
        return id == null ? "" : id;
    }

    /**
     * Set an attribute value on this element. If this element already has an attribute with the
     * key, its value is updated; otherwise, a new attribute is added.
     * 
     * @return this element
     */
    public Element attr(String attributeKey, String attributeValue) {
        super.attr(attributeKey, attributeValue);
        return this;
    }

    @Override
    public Element parent() {
        return (Element) super.parent();
    }

    /**
     * Get a child element of this element, by its 0-based index number.
     * <p/>
     * Note that an element can have both mixed Nodes and Elements as children. This method inspects
     * a filtered list of children that are elements, and the index is based on that filtered list.
     * 
     * @param index the index number of the element to retrieve
     * @return the child element, if it exists, or {@code null} if absent.
     * @see #childNode(int)
     */
    public Element child(int index) {
        return children().get(index);
    }

    /**
     * Get this element's child elements.
     * <p/>
     * This is effectively a filter on {@link #childNodes()} to get Element nodes.
     * @return child elements. If this element has no children, returns an
     * empty list.
     * @see #childNodes()
     */
    public Elements children() {
        // create on the fly rather than maintaining two lists. if gets slow, memoize, and mark dirty on change
        List<Element> elements = new ArrayList<Element>();
        for (Node node : childNodes) {
            if (node instanceof Element)
                elements.add((Element) node);
        }
        return new Elements(elements);
    }

    /**
     * Find elements that match the selector query, with this element as the starting context. Matched elements
     * may include this element, or any of its children.
     * <p/>
     * This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined, e.g.:
     * <ul>
     * <li>{@code el.select("a[href]")} - finds links ({@code a} tags with {@code href} attributes)
     * <li>{@code el.select("a[href*=example.com]")} - finds links pointing to example.com (loosely)
     * </ul>
     * <p/>
     * See the query syntax documentation in {@link org.jsoup.select.Selector}.
     *
     * @param query a {@link Selector} query
     * @return elements that match the query (empty if none match)
     * @see org.jsoup.select.Selector
     */
    public Elements select(String query) {
        return Selector.select(query, this);
    }
    
    /**
     * Add a node to the last child of this element.
     * 
     * @param child node to add. Must not already have a parent.
     * @return this element, so that you can add more child nodes or elements.
     */
    public Element appendChild(Node child) {
        Validate.notNull(child);
        
        child.setParentNode(this);
        childNodes.add(child);
        return this;
    }
    
    /**
     * Add a node to the start of this element's children.
     * 
     * @param child node to add. Must not already have a parent.
     * @return this element, so that you can add more child nodes or elements.
     */
    public Element prependChild(Node child) {
        Validate.notNull(child);
        
        child.setParentNode(this);
        childNodes.add(0, child);
        return this;
    }
    
    /**
     * Create a new element by tag name, and add it as the last child.
     * 
     * @param tagName the name of the tag (e.g. {@code div}).
     * @return the new element, to allow you to add content to it, e.g.:
     *  {@code parent.appendElement("h1").attr("id", "header").text("Welcome");}
     */
    public Element appendElement(String tagName) {
        Element child = new Element(Tag.valueOf(tagName), baseUri());
        appendChild(child);
        return child;
    }
    
    /**
     * Create a new element by tag name, and add it as the first child.
     * 
     * @param tagName the name of the tag (e.g. {@code div}).
     * @return the new element, to allow you to add content to it, e.g.:
     *  {@code parent.prependElement("h1").attr("id", "header").text("Welcome");}
     */
    public Element prependElement(String tagName) {
        Element child = new Element(Tag.valueOf(tagName), baseUri());
        prependChild(child);
        return child;
    }
    
    /**
     * Create and append a new TextNode to this element.
     * 
     * @param text the unencoded text to add
     * @return this element
     */
    public Element appendText(String text) {
        TextNode node = new TextNode(text, baseUri());
        appendChild(node);
        return this;
    }
    
    /**
     * Create and prepend a new TextNode to this element.
     * 
     * @param text the unencoded text to add
     * @return this element
     */
    public Element prependText(String text) {
        TextNode node = new TextNode(text, baseUri());
        prependChild(node);
        return this;
    }
    
    /**
     * Add inner HTML to this element. The supplied HTML will be parsed, and each node appended to the end of the children.
     * @param html HTML to add inside this element, after the existing HTML
     * @return this element
     * @see #html(String)
     */
    public Element append(String html) {
        Validate.notNull(html);
        
        Element fragment = Parser.parseBodyFragment(html, baseUri).body();
        // TODO: must parse without implicit elements, so you can e.g. add <td> to a <tr> (without creating a whole new table)
        for (Node node : fragment.childNodes()) {
            node.parentNode = null;
            appendChild(node);
        }
        return this;
    }
    
    /**
     * Add inner HTML to this element. The supplied HTML will be parsed, and each node prepended to the start of the children.
     * @param html HTML to add inside this element, before the existing HTML
     * @return this element
     * @see #html(String)
     */
    public Element prepend(String html) {
        Validate.notNull(html);
        
        Element fragment = Parser.parseBodyFragment(html, baseUri).body();
        // TODO: must parse without implicit elements, so you can e.g. add <td> to a <tr> (without creating a whole new table)
        List<Node> nodes = fragment.childNodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            node.parentNode = null;
            prependChild(node);
        }
        return this;
    }
    
    /**
     * Remove all of the element's child nodes. Any attributes are left as-is.
     * @return this element
     */
    public Element empty() {
        childNodes.clear();
        return this;
    }

    /**
     Wrap the supplied HTML around this element.
     @param html HTML to wrap around this element, e.g. {@code <div class="head"></div>}. Can be arbitralily deep.
     @return this element, for chaining.
     */
    public Element wrap(String html) {
        Validate.notEmpty(html);

        Element wrapBody = Parser.parseBodyFragment(html, baseUri).body();
        Elements wrapChildren = wrapBody.children();
        Element wrap = wrapChildren.first();
        if (wrap == null) // nothing to wrap with; noop
            return null;

        Element deepest = getDeepChild(wrap);
        parentNode.replaceChild(this, wrap);
        deepest.addChild(this);

        // remainder (unbalananced wrap, like <div></div><p></p> -- The <p> is remainder
        if (wrapChildren.size() > 1) {
            for (int i = 1; i < wrapChildren.size(); i++) { // skip first
                Element remainder = wrapChildren.get(i);
                remainder.parentNode.removeChild(remainder);
                wrap.appendChild(remainder);
            }
        }
        return this;
    }

    private Element getDeepChild(Element el) {
        List<Element> children = el.children();
        if (children.size() > 0)
            return getDeepChild(children.get(0));
        else
            return el;
    }
    
    /**
     * Get sibling elements.
     * @return sibling elements
     */
    public Elements siblingElements() {
        return parent().children();
    }

    /**
     * Gets the next sibling element of this element. E.g., if a {@code div} contains two {@code p}s, 
     * the {@code nextElementSibling} of the first {@code p} is the second {@code p}.
     * <p/>
     * This is similar to {@link #nextSibling()}, but specifically finds only Elements
     * @return the next element, or null if there is no next element
     * @see #previousElementSibling()
     */
    public Element nextElementSibling() {
        List<Element> siblings = parent().children();
        Integer index = indexInList(this, siblings);
        Validate.notNull(index);
        if (siblings.size() > index+1)
            return siblings.get(index+1);
        else
            return null;
    }

    /**
     * Gets the previous element sibling of this element.
     * @return the previous element, or null if there is no previous element
     * @see #nextElementSibling()
     */
    public Element previousElementSibling() {
        List<Element> siblings = parent().children();
        Integer index = indexInList(this, siblings);
        Validate.notNull(index);
        if (index > 0)
            return siblings.get(index-1);
        else
            return null;
    }

    /**
     * Gets the first element sibling of this element.
     * @return the first sibling that is an element (aka the parent's first element child) 
     */
    public Element firstElementSibling() {
        // todo: should firstSibling() exclude this?
        List<Element> siblings = parent().children();
        return siblings.size() > 1 ? siblings.get(0) : null;
    }
    
    /**
     * Get the list index of this element in its element sibling list. I.e. if this is the first element
     * sibling, returns 0.
     * @return position in element sibling list
     */
    public Integer elementSiblingIndex() {
       if (parent() == null) return 0;
       return indexInList(this, parent().children()); 
    }

    /**
     * Gets the last element sibling of this element
     * @return the last sibling that is an element (aka the parent's last element child) 
     */
    public Element lastElementSibling() {
        List<Element> siblings = parent().children();
        return siblings.size() > 1 ? siblings.get(siblings.size() - 1) : null;
    }

    // DOM type methods

    /**
     * Finds elements, including and recursively under this element, with the specified tag name.
     * @param tagName The tag name to search for (case insensitively).
     * @return a matching unmodifiable list of elements. Will be empty if this element and none of its children match.
     */
    public Elements getElementsByTag(String tagName) {
        Validate.notEmpty(tagName);
        tagName = tagName.toLowerCase().trim();

        return Collector.collect(new Evaluator.Tag(tagName), this);
    }

    /**
     * Find an element by ID, including or under this element.
     * <p>
     * Note that this finds the first matching ID, starting with this element. If you search down from a different
     * starting point, it is possible to find a different element by ID. For unique element by ID within a Document,
     * use {@link Document#getElementById(String)}
     * @param id The ID to search for.
     * @return The first matching element by ID, starting with this element, or null if none found.
     */
    public Element getElementById(String id) {
        Validate.notEmpty(id);
        
        Elements elements = Collector.collect(new Evaluator.Id(id), this);
        if (elements.size() > 0)
            return elements.get(0);
        else
            return null;
    }

    /**
     * Find elements that have this class, including or under this element. Case insensitive.
     * <p>
     * Elements can have multiple classes (e.g. {@code <div class="header round first">}. This method
     * checks each class, so you can find the above with {@code el.getElementsByClass("header");}.
     * 
     * @param className the name of the class to search for.
     * @return elements with the supplied class name, empty if none
     * @see #hasClass(String)
     * @see #classNames()
     */
    public Elements getElementsByClass(String className) {
        Validate.notEmpty(className);

        return Collector.collect(new Evaluator.Class(className), this);
    }

    /**
     * Find elements that have a named attribute set. Case insensitive.
     *
     * @param key name of the attribute
     * @return elements that have this attribute, empty if none
     */
    public Elements getElementsByAttribute(String key) {
        Validate.notEmpty(key);
        key = key.trim().toLowerCase();

        return Collector.collect(new Evaluator.Attribute(key), this);
    }

    /**
     * Find elements that have an attribute with the specific value. Case insensitive.
     * 
     * @param key name of the attribute
     * @param value value of the attribute
     * @return elements that have this attribute with this value, empty if none
     */
    public Elements getElementsByAttributeValue(String key, String value) {
        return Collector.collect(new Evaluator.AttributeWithValue(key, value), this);
    }

    /**
     * Find elements that either do not have this attribute, or have it with a different value. Case insensitive.
     * 
     * @param key name of the attribute
     * @param value value of the attribute
     * @return elements that do not have a matching attribute
     */
    public Elements getElementsByAttributeValueNot(String key, String value) {
        return Collector.collect(new Evaluator.AttributeWithValueNot(key, value), this);
    }

    /**
     * Find elements that have attributes that start with the value prefix. Case insensitive.
     * 
     * @param key name of the attribute
     * @param valuePrefix start of attribute value
     * @return elements that have attributes that start with the value prefix
     */
    public Elements getElementsByAttributeValueStarting(String key, String valuePrefix) {
        return Collector.collect(new Evaluator.AttributeWithValueStarting(key, valuePrefix), this);
    }

    /**
     * Find elements that have attributes that end with the value suffix. Case insensitive.
     * 
     * @param key name of the attribute
     * @param valueSuffix end of the attribute value
     * @return elements that have attributes that end with the value suffix
     */
    public Elements getElementsByAttributeValueEnding(String key, String valueSuffix) {
        return Collector.collect(new Evaluator.AttributeWithValueEnding(key, valueSuffix), this);
    }

    /**
     * Find elements that have attributes whose value contains the match string. Case insensitive.
     * 
     * @param key name of the attribute
     * @param match substring of value to search for
     * @return elements that have attributes containing this text
     */
    public Elements getElementsByAttributeValueContaining(String key, String match) {
        return Collector.collect(new Evaluator.AttributeWithValueContaining(key, match), this);
    }
    
    /**
     * Find elements whose sibling index is less than the supplied index.
     * @param index 0-based index
     * @return elements less than index
     */
    public Elements getElementsByIndexLessThan(int index) {
        return Collector.collect(new Evaluator.IndexLessThan(index), this);
    }
    
    /**
     * Find elements whose sibling index is greater than the supplied index.
     * @param index 0-based index
     * @return elements greater than index
     */
    public Elements getElementsByIndexGreaterThan(int index) {
        return Collector.collect(new Evaluator.IndexGreaterThan(index), this);
    }
    
    /**
     * Find elements whose sibling index is equal to the supplied index.
     * @param index 0-based index
     * @return elements equal to index
     */
    public Elements getElementsByIndexEquals(int index) {
        return Collector.collect(new Evaluator.IndexEquals(index), this);
    }
    
    /**
     * Find all elements under this element (including self, and children of children).
     * 
     * @return all elements
     */
    public Elements getAllElements() {
        return Collector.collect(new Evaluator.AllElements(), this);
    }

    /**
     * Gets the combined text of this element and all its children.
     * 
     * @return unencoded text, or empty string if none.
     */
    public String text() {
        StringBuilder sb = new StringBuilder();
        text(sb);
        return sb.toString().trim();
    }

    private void text(StringBuilder accum) {
        for (Node child : childNodes) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                String text = textNode.getWholeText();

                if (!preserveWhitespace()) {
                    text = TextNode.normaliseWhitespace(text);
                    if (TextNode.lastCharIsWhitespace(accum))
                        text = TextNode.stripLeadingWhitespace(text);
                }
                accum.append(text);

            } else if (child instanceof Element) {
                Element element = (Element) child;
                if (accum.length() > 0 && element.isBlock() && !TextNode.lastCharIsWhitespace(accum))
                    accum.append(" ");
                element.text(accum);
            }
        }
    }

    boolean preserveWhitespace() {
        return tag.preserveWhitespace() || parent() != null && parent().preserveWhitespace();
    }

    /**
     * Set the text of this element. Any existing contents (text or elements) will be cleared
     * @param text unencoded text
     * @return this element
     */
    public Element text(String text) {
        Validate.notNull(text);

        empty();
        TextNode textNode = new TextNode(text, baseUri);
        appendChild(textNode);

        return this;
    }

    /**
     Test if this element has any text content (that is not just whitespace).
     @return true if element has non-blank text content.
     */
    public boolean hasText() {
        for (Node child: childNodes) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                if (!textNode.isBlank())
                    return true;
            } else if (child instanceof Element) {
                Element el = (Element) child;
                if (el.hasText())
                    return true;
            }
        }
        return false;
    }

    /**
     * Get the combined data of this element. Data is e.g. the inside of a {@code script} tag.
     * @return the data, or empty string if none
     */
    public String data() {
        StringBuilder sb = new StringBuilder();

        for (Node childNode : childNodes) {
            if (childNode instanceof DataNode) {
                DataNode data = (DataNode) childNode;
                sb.append(data.getWholeData());
            } else if (childNode instanceof Element) {
                Element element = (Element) childNode;
                String elementData = element.data();
                sb.append(elementData);
            }
        }
        return sb.toString();
    }   

    /**
     * Gets the literal value of this element's "class" attribute, which may include multiple class names, space
     * separated. (E.g. on <code>&lt;div class="header gray"></code> returns, "<code>header gray</code>")
     * @return The literal class attribute, or <b>empty string</b> if no class attribute set.
     */
    public String className() {
        return attributes.hasKey("class") ? attributes.get("class") : "";
    }

    /**
     * Get all of the element's class names. E.g. on element {@code <div class="header gray"}>},
     * returns a set of two elements {@code "header", "gray"}. Note that modifications to this set are not pushed to
     * the backing {@code class} attribute; use the {@link #classNames(java.util.Set)} method to persist them.
     * @return set of classnames, empty if no class attribute
     */
    public Set<String> classNames() {
        if (classNames == null) {
            String[] names = className().split("\\s+");
            classNames = new LinkedHashSet<String>(Arrays.asList(names));
        }
        return classNames;
    }

    /**
     Set the element's {@code class} attribute to the supplied class names.
     @param classNames set of classes
     @return this element, for chaining
     */
    public Element classNames(Set<String> classNames) {
        Validate.notNull(classNames);
        attributes.put("class", StringUtils.join(classNames, " "));
        return this;
    }

    /**
     * Tests if this element has a class.
     * @param className name of class to check for
     * @return true if it does, false if not
     */
    public boolean hasClass(String className) {
        return classNames().contains(className);
    }

    /**
     Add a class name to this element's {@code class} attribute.
     @param className class name to add
     @return this element
     */
    public Element addClass(String className) {
        Validate.notNull(className);

        Set<String> classes = classNames();
        classes.add(className);
        classNames(classes);

        return this;
    }

    /**
     Remove a class name from this element's {@code class} attribute.
     @param className class name to remove
     @return this element
     */
    public Element removeClass(String className) {
        Validate.notNull(className);

        Set<String> classes = classNames();
        classes.remove(className);
        classNames(classes);

        return this;
    }

    /**
     Toggle a class name on this element's {@code class} attribute: if present, remove it; otherwise add it.
     @param className class name to toggle
     @return this element
     */
    public Element toggleClass(String className) {
        Validate.notNull(className);

        Set<String> classes = classNames();
        if (classes.contains(className))
            classes.remove(className);
        else
            classes.add(className);
        classNames(classes);

        return this;
    }
    
    /**
     * Get the value of a form element (input, textarea, etc).
     * @return the value of the form element, or empty string if not set.
     */
    public String val() {
        if (tagName().equals("textarea"))
            return text();
        else
            return attr("value");
    }
    
    /**
     * Set the value of a form element (input, textarea, etc).
     * @param value value to set
     * @return this element (for chaining)
     */
    public Element val(String value) {
        if (tagName().equals("textarea"))
            text(value);
        else
            attr("value", value);
        return this;
    }

    void outerHtml(StringBuilder accum) {
        if (isBlock() || (parent() != null && parent().tag().canContainBlock() && siblingIndex() == 0))
            indent(accum);
        accum
                .append("<")
                .append(tagName())
                .append(attributes.html());

        if (childNodes.isEmpty() && tag.isEmpty()) {
            accum.append(" />");
        } else {
            accum.append(">");
            html(accum);
            if (tag.canContainBlock()) indent(accum);
            accum.append("</").append(tagName()).append(">");
        }
    }

    /**
     * Retrieves the element's inner HTML. E.g. on a {@code <div>} with one empty {@code <p>}, would return
     * {@code <p></p>}. (Whereas {@link #outerHtml()} would return {@code <div><p></p></div>}.)
     * 
     * @return String of HTML.
     * @see #outerHtml()
     */
    public String html() {
        StringBuilder accum = new StringBuilder();
        html(accum); 
        return accum.toString().trim();
    }

    private void html(StringBuilder accum) {
        for (Node node : childNodes)
            node.outerHtml(accum);
    }
    
    /**
     * Set this element's inner HTML. Clears the existing HTML first.
     * @param html HTML to parse and set into this element
     * @return this element
     * @see #append(String)
     */
    public Element html(String html) {
        empty();
        append(html);
        return this;
    }

    public String toString() {
        return outerHtml();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Element)) return false;
        if (!super.equals(o)) return false;

        Element element = (Element) o;

        if (tag != null ? !tag.equals(element.tag) : element.tag != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }
}
