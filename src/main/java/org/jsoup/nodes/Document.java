package org.jsoup.nodes;

import org.apache.commons.lang.Validate;
import org.jsoup.parser.Tag;

import java.util.List;
import java.util.ArrayList;

/**
 A HTML Document.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Document extends Element {

    /**
     Create a new, empty Document.
     @param baseUri base URI of document
     @see org.jsoup.Jsoup#parse
     @see #createShell
     */
    public Document(String baseUri) {
        super(Tag.valueOf("#root"), baseUri);
    }

    /**
     Create a valid, empty shell of a document, suitable for adding more elements to.
     @param baseUri baseUri of document
     @return document with html, head, and body elements.
     */
    static public Document createShell(String baseUri) {
        Validate.notNull(baseUri);

        Document doc = new Document(baseUri);
        Element html = doc.appendElement("html");
        html.appendElement("head");
        html.appendElement("body");

        return doc;
    }

    /**
     Accessor to the document's {@code head} element.
     @return {@code head}
     */
    public Element head() {
        return getElementsByTag("head").first();
    }

    /**
     Accessor to the document's {@code body} element.
     @return {@code body}
     */
    public Element body() {
        return getElementsByTag("body").first();
    }

    /**
     Get the string contents of the document's {@code title} element.
     @return Trimed title, or empty string if none set.
     */
    public String title() {
        Element titleEl = getElementsByTag("title").first();
        return titleEl != null ? titleEl.text().trim() : "";
    }

    /**
     Set the document's {@code title} element. Updates the existing element, or adds {@code title} to {@code head} if
     not present
     @param title string to set as title
     */
    public void title(String title) {
        Validate.notNull(title);
        Element titleEl = getElementsByTag("title").first();
        if (titleEl == null) { // add to head
            head().appendElement("title").text(title);
        } else {
            titleEl.text(title);
        }
    }

    /**
     Create a new Element, with this document's base uri. Does not make the new element a child of this document.
     @param tagName element tag name (e.g. {@code a})
     @return new element
     */
    public Element createElement(String tagName) {
        return new Element(Tag.valueOf(tagName), this.baseUri());
    }

    /**
     Normalise the document. This happens after the parse phase so generally does not need to be called.
     Moves any text content that is not in the body element into the body.
     @return this document after normalisation
     */
    public Document normalise() {
        if (select("html").isEmpty())
            appendElement("html");
        if (head() == null)
            select("html").first().prependElement("head");
        if (body() == null)
            select("html").first().appendElement("body");

        // pull text nodes out of root, html, and head els, and push into body. non-text nodes are already taken care
        // of. do in inverse order to maintain text order.
        normalise(head());
        normalise(select("html").first());
        normalise(this);        

        return this;
    }

    // does not recurse.
    private void normalise(Element element) {
        List<Node> toMove = new ArrayList<Node>();
        for (Node node: element.childNodes) {
            if (node instanceof TextNode) {
                TextNode tn = (TextNode) node;
                if (!tn.isBlank())
                    toMove.add(tn);
            }
        }

        for (Node node: toMove) {
            element.removeChild(node);
            body().prependChild(node);
            body().prependChild(new TextNode(" ", ""));
        }
    }

    @Override
    public String outerHtml() {
        return super.html(); // no outer wrapper tag
    }

    /**
     Set the text of the {@code body} of this document. Any existing nodes within the body will be cleared.
     @param text unencoded text
     @return this document
     */
    @Override
    public Element text(String text) {
        body().text(text); // overridden to not nuke doc structure
        return this;
    }

    @Override
    public String nodeName() {
        return "#document";
    }
}

