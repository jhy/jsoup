package org.jsoup.select;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.parser.TokenQueue;

import java.util.LinkedHashSet;
import java.util.List;


/**
 TODO: Document

 @author Jonathan Hedley, jonathan@hedley.net */
public class Selector {
    private final Element root;
    private final LinkedHashSet<Element> elements; // LHS for unique and ordered elements
    private final String query;
    private final TokenQueue tq;

    private Selector(String query, Element root) {
        Validate.notEmpty(query);
        Validate.notNull(root);

        this.elements = new LinkedHashSet<Element>();
        this.query = query.trim();
        this.root = root;
        this.tq = new TokenQueue(query);
    }

    public static Elements select(String query, Element root) {
        return new Selector(query, root).select();
    }

    public static Elements select(String query, Iterable<Element> roots) {
        Validate.notEmpty(query);
        Validate.notNull(roots);
        LinkedHashSet<Element> elements = new LinkedHashSet<Element>();

        for (Element root : roots) {
            elements.addAll(select(query, root));
        }
        return new Elements(elements);
    }

    private Elements select() {
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
            } else if (tq.matchChomp(",")) {
                groupOr();
            } else { // unhandled
                throw new SelectorParseException("Could not parse query " + query);
            }
        }
        return new Elements(elements);
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

    private void groupOr() {
        // no-op; just append uniques
    }

    public static class SelectorParseException extends IllegalStateException {
        public SelectorParseException(String s) {
            super(s);
        }
    }
}
