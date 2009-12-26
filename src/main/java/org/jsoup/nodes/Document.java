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
    }
}
