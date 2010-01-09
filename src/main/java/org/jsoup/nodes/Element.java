package org.jsoup.nodes;

import org.apache.commons.lang.Validate;
import org.jsoup.parser.StartTag;
import org.jsoup.parser.Tag;
import org.jsoup.select.Collector;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;

import java.util.*;

/**
 A HTML element: tag + data, e.g. <code>&lt;div id="foo"&gt;content&lt;/div&gt;</code>

 @author Jonathan Hedley, jonathan@hedley.net */
public class Element extends Node {
    private final Tag tag;
    private final List<Element> elementChildren; // subset of Node.children, only holds Elements
    private Set<String> classNames;

    public Element(StartTag startTag) {
        super(startTag.getBaseUri(), startTag.getAttributes());
        this.tag = startTag.getTag();
        elementChildren = new ArrayList<Element>();
    }

    public Element(Tag tag, String baseUri) {
        this(new StartTag(tag, baseUri));
    }

    public String nodeName() {
        return tag.getName();
    }

    public String tagName() {
        return tag.getName();
    }

    public Tag getTag() {
        return tag;
    }

    public boolean isBlock() {
        return tag.isBlock();
    }

    public String id() {
        String id = attr("id");
        return id == null ? "" : id;
    }

    @Override
    public Element attr(String attributeKey, String attributeValue) {
        super.attr(attributeKey, attributeValue);
        return this;
    }

    @Override
    public Element parent() {
        return (Element) super.parent();
    }

    public Element child(int index) {
        return elementChildren.get(index);
    }

    public List<Element> children() {
        return Collections.unmodifiableList(elementChildren);
    }

    public Elements select(String query) {
        return Selector.select(query, this);
    }

    public void addChild(Element child) {
        Validate.notNull(child);
        elementChildren.add(child);
        childNodes.add(child);
        child.setParentNode(this);
    }

    public void addChild(Node child) {
        Validate.notNull(child);
        childNodes.add(child);
        child.setParentNode(this);
    }

    public Element nextElementSibling() {
        List<Element> siblings = parent().elementChildren;
        Integer index = indexInList(this, siblings);
        Validate.notNull(index);
        if (siblings.size() > index+1)
            return siblings.get(index+1);
        else
            return null;
    }

    public Element previousElementSibling() {
        List<Element> siblings = parent().elementChildren;
        Integer index = indexInList(this, siblings);
        Validate.notNull(index);
        if (index > 0)
            return siblings.get(index-1);
        else
            return null;
    }

    public Element firstElementSibling() {
        // todo: should firstSibling() exclude this?
        List<Element> siblings = parent().elementChildren;
        return siblings.size() > 1 ? siblings.get(0) : null;
    }

    public Element lastElementSibling() {
        List<Element> siblings = parent().elementChildren;
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
     * starting point, it is possible to find a different element by ID. For unique element by ID withing a Document,
     * use Document.getElementById.
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

    public Elements getElementsByClass(String className) {
        Validate.notEmpty(className);

        return Collector.collect(new Evaluator.Class(className), this);
    }

    public Elements getElementsByAttribute(String key) {
        Validate.notEmpty(key);
        key = key.trim().toLowerCase();

        return Collector.collect(new Evaluator.Attribute(key), this);
    }

    public Elements getElementsByAttributeValue(String key, String value) {
        String[] kp = normaliseAttrKeyPair(key, value);
        return Collector.collect(new Evaluator.AttributeWithValue(kp[0], kp[1]), this);
    }

    public Elements getElementsByAttributeValueNot(String key, String value) {
        String[] kp = normaliseAttrKeyPair(key, value);
        return Collector.collect(new Evaluator.AttributeWithValueNot(kp[0], kp[1]), this);
    }

    public Elements getElementsByAttributeValueStarting(String key, String value) {
        String[] kp = normaliseAttrKeyPair(key, value);
        return Collector.collect(new Evaluator.AttributeWithValueStarting(kp[0], kp[1]), this);
    }

    public Elements getElementsByAttributeValueEnding(String key, String value) {
        String[] kp = normaliseAttrKeyPair(key, value);
        return Collector.collect(new Evaluator.AttributeWithValueEnding(kp[0], kp[1]), this);
    }

    public Elements getElementsByAttributeValueContaining(String key, String value) {
        String[] kp = normaliseAttrKeyPair(key, value);
        return Collector.collect(new Evaluator.AttributeWithValueContaining(kp[0], kp[1]), this);
    }

    private String[] normaliseAttrKeyPair(String key, String value) {
        Validate.notEmpty(key);
        key = key.trim().toLowerCase();
        Validate.notEmpty(value);
        value = value.trim().toLowerCase();
        return new String[] {key, value};
    }

    public String text() {
        StringBuilder sb = new StringBuilder();

        for (Node childNode : childNodes) {
            if (childNode instanceof TextNode) {
                TextNode textNode = (TextNode) childNode;
                sb.append(textNode.getWholeText());
            } else if (childNode instanceof Element) {
                Element element = (Element) childNode;
                String elementText = element.text();
                if (element.isBlock() && sb.length() > 0 && elementText.length() > 0)
                    sb.append(" ");
                sb.append(elementText);
            }
        }
        return sb.toString();
    }

    public Element text(String text) {
        Validate.notNull(text);

        childNodes.clear();
        elementChildren.clear();
        TextNode textNode = new TextNode(text, baseUri);
        addChild(textNode);

        return this;
    }

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
     * separated (e.g. <code>&lt;div class="header gray"></code> returns "<code>header gray</code>")
     * @return The literal class attribute, or <b>empty string</b> if no class attribute set.
     */
    public String className() {
        return attributes.hasKey("class") ? attributes.get("class") : "";
    }

    public Set<String> classNames() {
        if (classNames == null) {
            String[] names = className().split("\\s+");
            classNames = new HashSet<String>(Arrays.asList(names));
        }
        return classNames;
    }

    public boolean hasClass(String className) {
        return classNames().contains(className);
    }

    public String outerHtml() {
        StringBuilder accum = new StringBuilder();
        accum
                .append("<")
                .append(tagName())
                .append(attributes.html());

        if (childNodes.isEmpty() && tag.isEmpty()) {
            accum.append(" />");
        } else {
            accum.append(">");
            accum.append(html());
            accum.append("</").append(tagName()).append(">");
        }

        return accum.toString();
    }

    /**
     * Retrieves the element's inner HTML.
     * @return String of HTML.
     */
    public String html() {
        StringBuilder accum = new StringBuilder();
        for (Node node : childNodes)
            accum.append(node.outerHtml());

        return accum.toString();
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
