package org.jsoup.select;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.parser.TokenQueue;

import java.util.Collection;
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
        Validate.notNull(query);
        query = query.trim();
        Validate.notEmpty(query);
        Validate.notNull(root);

        this.elements = new LinkedHashSet<Element>();
        this.query = query;
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

        if (tq.matchChomp("#")) {
            byId();
        } else if (tq.matchChomp(".")) {
            byClass();
        } else if (tq.matchesWord()) {
            byTag();
        } else if (tq.matchChomp("[")) {
            byAttribute();
        } else if (tq.matchChomp("*")) {
            allElements();
        } else { // unhandled
            throw new SelectorParseException("Could not parse query " + query);
        }

        // hierarchy (todo: implement +, ~)
        boolean seenWhite = tq.consumeWhitespace();
        if (!tq.isEmpty()) { 
            if (tq.matchChomp(",")) { // group or
                while (!tq.isEmpty()) {
                    String subQuery = tq.chompTo(",");
                    elements.addAll(select(subQuery, root));
                }
                return new Elements(elements);
            } else if (tq.matchChomp(">")) { // parent > child
                Elements candidates = new Elements(select(tq.remainder(), elements));
                return filterForChildren(elements, candidates);
            } else if (seenWhite) { // ancestor descendant
                Elements candidates = new Elements(select(tq.remainder(), elements));
                return filterForDescendants(elements, candidates);
            } else { // E.class, E#id, E[attr] etc. AND
                Elements candidates = new Elements(select(tq.remainder(), elements));
                return filterForSelf(elements, candidates);
            }
        } else {
            return new Elements(elements);
        }
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

        List<Element> found = root.getElementsByClass(className);
        elements.addAll(found);
    }

    private void byTag() {
        String tagName = tq.consumeWord();
        Validate.notEmpty(tagName);

        elements.addAll(root.getElementsByTag(tagName));
    }

    private void byAttribute() {
        String key = tq.consumeToAny("=", "!=", "^=", "$=", "*=", "]"); // eq, not, start, end, contain, (no val)
        Validate.notEmpty(key);

        if (tq.matchChomp("]")) {
            elements.addAll(root.getElementsByAttribute(key));
        } else {
            if (tq.matchChomp("="))
                elements.addAll(root.getElementsByAttributeValue(key, tq.chompTo("]")));

            else if (tq.matchChomp("!="))
                elements.addAll(root.getElementsByAttributeValueNot(key, tq.chompTo("]")));

            else if (tq.matchChomp("^="))
                elements.addAll(root.getElementsByAttributeValueStarting(key, tq.chompTo("]")));

            else if (tq.matchChomp("$="))
                elements.addAll(root.getElementsByAttributeValueEnding(key, tq.chompTo("]")));

            else if (tq.matchChomp("*="))
                elements.addAll(root.getElementsByAttributeValueContaining(key, tq.chompTo("]")));
        }
    }

    private void allElements() {
        elements.addAll(Collector.collect(new Evaluator.AllElements(), root));
    }

    // direct child descendants
    private static Elements filterForChildren(Collection<Element> parents, Collection<Element> candidates) {
        Elements children = new Elements();
        CHILD: for (Element c : candidates) {
            for (Element p : parents) {
                if (c.parent().equals(p)) {
                    children.add(c);
                    continue CHILD;
                }
            }
        }
        return children;
    }
    
    // children or lower descendants. input candidates stemmed from found elements, so are either a descendant 
    // or the original element; so check that parent is not child
    private static Elements filterForDescendants(Collection<Element> parents, Collection<Element> candidates) {
        Elements children = new Elements();
        CHILD: for (Element c : candidates) {
            boolean found = false;
            for (Element p : parents) {
                if (c.equals(p)) {
                    found = true;
                    continue CHILD;
                }
            }
            if (!found)
                children.add(c);
        }
        return children;
    }
    
    // union of both sets, for e.class type selectors
    private static Elements filterForSelf(Collection<Element> parents, Collection<Element> candidates) {
        Elements children = new Elements();
        CHILD: for (Element c : candidates) {
            for (Element p : parents) {
                if (c.equals(p)) {
                    children.add(c);
                    continue CHILD;
                }
            }   
        }
        return children;
    }

    public static class SelectorParseException extends IllegalStateException {
        public SelectorParseException(String s) {
            super(s);
        }
    }
}
