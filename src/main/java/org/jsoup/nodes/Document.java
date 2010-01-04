package org.jsoup.nodes;

import org.jsoup.parser.StartTag;
import org.jsoup.parser.Tag;

/**
 Document element.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Document extends Element {
    private String title;

    public Document() {
        super(new StartTag(Tag.valueOf("#root")));
    }

    public Element getHead() {
        return getElementsByTag("head").get(0);
    }

    public Element getBody() {
        return getElementsByTag("body").get(0);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String outerHtml() {
        return super.html(); // no outer wrapper tag
    }
}

