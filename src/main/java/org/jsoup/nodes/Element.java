package org.jsoup.nodes;

import org.apache.commons.lang.Validate;
import org.jsoup.parser.StartTag;
import org.jsoup.parser.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 A HTML element: tag + data, e.g. <code>&lt;div id="foo"&gt;content&lt;/div&gt;</code>

 @author Jonathan Hedley, jonathan@hedley.net */
public class Element extends Node {
    private final Tag tag;
    private final List<Element> elementChildren; // subset of Node.children, only holds Elements

    public Element(Node parentNode, StartTag startTag) {
        super(parentNode, startTag.getAttributes());
        this.tag = startTag.getTag();
        elementChildren = new ArrayList<Element>();
    }

    public String getNodeName() {
        return tag.getName();
    }

    public String getTagName() {
        return tag.getName();
    }

    public Tag getTag() {
        return tag;
    }

    public List<Element> getChildren() {
        return Collections.unmodifiableList(elementChildren);
    }

    public void addChild(Element child) {
        Validate.notNull(child);
        elementChildren.add(child);
        childNodes.add(child);
    }

    public void addChild(Node child) {
        Validate.notNull(child);
        childNodes.add(child);
    }

    public Element getNextElementSibling() {
        // TODO: implement
        return null;
    }

    public Element getPreviousElementSibling() {
        // TODO: implement
        return null;
    }


}
