package org.jsoup.nodes;

import org.jsoup.parser.StartTag;
import org.jsoup.parser.Tag;

/**
 Document element.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Document extends Element {
    private Element head;
    private Element body;
    private String title;

    public Document() {
        super(new StartTag(Tag.valueOf("html")));
        head = new Element (new StartTag(Tag.valueOf("head")));
        body = new Element (new StartTag(Tag.valueOf("body")));

        this.addChild(head);
        this.addChild(body);
    }

    public Element getHead() {
        return head;
    }

    public Element getBody() {
        return body;
    }
}

