package org.jsoup.nodes;

import org.apache.commons.lang.Validate;
import org.jsoup.parser.StartTag;
import org.jsoup.parser.Tag;

/**
 Document element.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Document extends Element {
    private String title;

    public Document(String baseUri) {
        super(new StartTag(Tag.valueOf("#root"), baseUri));
    }

    /**
     Create a valid, empty shell of a document, suitable for adding more elements to (without parsing).
     @param baseUri baseUri of document
     @return document with html, head, and body elements.
     */
    static public Document createShell(String baseUri) {
        Validate.notNull(baseUri);

        Document doc = new Document(baseUri);
        Element html = doc.createElement(Tag.valueOf("html"));
        Element head = doc.createElement(Tag.valueOf("head"));
        Element body = doc.createElement(Tag.valueOf("body"));

        doc.appendChild(html);
        html.appendChild(head);
        html.appendChild(body);

        return doc;
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

    public Element createElement(Tag tag) {
        return new Element(tag, baseUri());
    }

    @Override
    public String outerHtml() {
        return super.html(); // no outer wrapper tag
    }

    @Override
    public Element text(String text) {
        getBody().text(text); // overridden to not nuke doc structure
        return this;
    }

    @Override
    public String nodeName() {
        return "#document";
    }
    
    
}

