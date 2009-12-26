package org.jsoup.nodes;

import org.apache.commons.lang.Validate;
import org.jsoup.parser.StartTag;
import org.jsoup.parser.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 A HTML element: tag + data, e.g. <code>&lt;div id="foo"&gt;content&lt;/div&gt;</code>

 @author Jonathan Hedley, jonathan@hedley.net */
public class Element extends Node {
    private final Tag tag;
    private final List<Element> elementChildren; // subset of Node.children, only holds Elements

    public Element(StartTag startTag) {
        super(startTag.getAttributes());
        this.tag = startTag.getTag();
        elementChildren = new ArrayList<Element>();
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

    public String id() {
        String id = attr("id");
        return id == null ? "" : id;
    }

    @Override
    public Element attr(String attributeKey, String attributeValue) {
        super.attr(attributeKey, attributeValue);
        return this;
    }

    public Element child(int index) {
        return elementChildren.get(index);
    }

    public List<Element> children() {
        return Collections.unmodifiableList(elementChildren);
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
        // TODO: implement
        return null;
    }

    public Element previousElementSibling() {
        // TODO: implement
        return null;
    }

    // DOM type methods

    /**
     * Finds elements, including and recursively under this element, with the specified tag name.
     * @param tagName The tag name to search for (case insensitively).
     * @return a matching unmodifiable list of elements. Will be empty if this element and none of its children match.
     */
    public List<Element> getElementsByTag(String tagName) {
        Validate.notEmpty(tagName);
        tagName = tagName.toLowerCase().trim();

        List<Element> elements = new ArrayList<Element>();
        if (this.tag.getName().equals(tagName))
            elements.add(this);
        for (Element child : elementChildren) {
            elements.addAll(child.getElementsByTag(tagName));
        }
        return Collections.unmodifiableList(elements);
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
        
        if (this.id().equals(id))
            return this;
        for (Element child : elementChildren) {
            Element byId = child.getElementById(id);
            if (byId != null)
                return byId;
        }
        return null;
    }


}
