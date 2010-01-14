package org.jsoup.select;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;
import org.jsoup.parser.TokenQueue;

import java.util.Collection;
import java.util.LinkedHashSet;

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
        addElements(findElements()); // chomp first matcher off queue        
        while (!tq.isEmpty()) {
            // hierarchy and extras (todo: implement +, ~)
            boolean seenWhite = tq.consumeWhitespace();
            
            if (tq.matchChomp(",")) { // group or
                while (!tq.isEmpty()) {
                    String subQuery = tq.chompTo(",");
                    elements.addAll(select(subQuery, root));
                }
            } else if (tq.matchChomp(">")) { // parent > child
                String subQuery = tq.chompTo(">"); // support multi > childs
                Elements candidates = select(subQuery, elements);
                Elements children = filterForChildren(elements, candidates);
                elements.clear(); elements.addAll(children);
            } else if (seenWhite) { // ancestor descendant
                Elements candidates = select(tq.remainder(), elements);
                return filterForDescendants(elements, candidates);
            } else { // E.class, E#id, E[attr] etc. AND
                Elements candidates = findElements(); // take next el, #. etc off queue
                intersectElements(filterForSelf(elements, candidates));
            }
        }
        return new Elements(elements);
    }
    
    private Elements findElements() {
        if (tq.matchChomp("#")) {
            return byId();
        } else if (tq.matchChomp(".")) {
            return byClass();
        } else if (tq.matchesWord()) {
            return byTag();
        } else if (tq.matchChomp("[")) {
            return byAttribute();
        } else if (tq.matchChomp("*")) {
            return allElements();
        } else { // unhandled
            throw new SelectorParseException("Could not parse query " + query);
        }
    }
    
    private void addElements(Collection<Element> add) {
        elements.addAll(add);
    }
    
    private void intersectElements(Collection<Element> intersect) {
        elements.retainAll(intersect);
    }

    private Elements byId() {
        String id = tq.consumeWord();
        Validate.notEmpty(id);

        Element found = root.getElementById(id);
        Elements byId = new Elements();
        if(found != null)
            byId.add(found);
        return byId;
    }

    private Elements byClass() {
        String className = tq.consumeClassName();
        Validate.notEmpty(className);

        return root.getElementsByClass(className);
    }

    private Elements byTag() {
        String tagName = tq.consumeWord();
        Validate.notEmpty(tagName);

        return root.getElementsByTag(tagName);
    }

    private Elements byAttribute() {
        String key = tq.consumeToAny("=", "!=", "^=", "$=", "*=", "]"); // eq, not, start, end, contain, (no val)
        Validate.notEmpty(key);

        if (tq.matchChomp("]")) {
            return root.getElementsByAttribute(key);
        } else {
            if (tq.matchChomp("="))
                return root.getElementsByAttributeValue(key, tq.chompTo("]"));

            else if (tq.matchChomp("!="))
                return root.getElementsByAttributeValueNot(key, tq.chompTo("]"));

            else if (tq.matchChomp("^="))
                return root.getElementsByAttributeValueStarting(key, tq.chompTo("]"));

            else if (tq.matchChomp("$="))
                return root.getElementsByAttributeValueEnding(key, tq.chompTo("]"));

            else if (tq.matchChomp("*="))
                return root.getElementsByAttributeValueContaining(key, tq.chompTo("]"));
            
            else
                throw new SelectorParseException("Could not parse attribute query " + query);
        }
    }

    private Elements allElements() {
        return root.getAllElements();
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
