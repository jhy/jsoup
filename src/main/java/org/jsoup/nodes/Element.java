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
        return attr("id");
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

    public List<Element> getElementsByTag(String tagName) {
        Validate.notEmpty(tagName);
        tagName = tagName.toLowerCase().trim();

        List<Element> elements = new ArrayList<Element>();
        if (tag.getName().equals(tagName))
            elements.add(this);
        for (Element child : elementChildren) {
            elements.addAll(child.getElementsByTag(tagName));
        }
        return elements;
    }


}
