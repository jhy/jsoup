package org.jsoup.select;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.parser.TokenQueue;

import java.util.List;


/**
 TODO: Document

 @author Jonathan Hedley, jonathan@hedley.net */
public class Selector {
    private final Element root;
    private final ElementList elements;
    private final String query;
    private final TokenQueue tq;

    private Selector(String query, Element root) {
        Validate.notEmpty(query);
        Validate.notNull(root);

        this.elements = new ElementList();
        this.query = query.trim();
        this.root = root;
        this.tq = new TokenQueue(query);
    }

    public static ElementList select(String query, Element root) {
        return new Selector(query, root).select();
    }

    private ElementList select() {
        tq.consumeWhitespace();
        while (!tq.isEmpty()) {
            if (tq.matchChomp("#")) {
                byId();
            } else if (tq.matchChomp(".")) {
                byClass();
            } else if (tq.matchesWord()) {
                byTag();
            } else if (tq.matchChomp("[")) {
                byAttribute();
            } else { // unhandled
                throw new SelectorParseException("Could not parse query " + query);
            }
        }
        return elements;
    }

    private void byId() {
        String id = tq.consumeWord();
        Validate.notEmpty(id);

        Element found = root.getElementById(id);
        if(found != null)
            elements.add(found);
    }

    private void byClass() {
        String className = tq.consumeClassName();
        Validate.notEmpty(className);

        List<Element> found = root.getElementsWithClass(className);
        elements.addAll(found);
    }

    private void byTag() {
        String tagName = tq.consumeWord();
        Validate.notEmpty(tagName);

        elements.addAll(root.getElementsByTag(tagName));
    }

    private void byAttribute() {
        String key = tq.consumeToAny("=", "]");
        Validate.notEmpty(key);
        String value = null;
        if (tq.matchChomp("="))
            value = tq.chompTo("]");
        else
            tq.consume("]");

        if (value != null)
            elements.addAll(root.getElementsWithAttributeValue(key, value));
        else {
            elements.addAll(root.getElementsWithAttribute(key));
        }

    }

    public static class SelectorParseException extends IllegalStateException {
        public SelectorParseException(String s) {
            super(s);
        }
    }
}
