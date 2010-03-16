package org.jsoup.select;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.parser.TokenQueue;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 CSS-like element selector, that finds elements matching a query.

 <h2>Selector syntax</h2>
 A selector is a chain of simple selectors, seperated by combinators. Selectors are case insensitive (including against
 elements, attributes, and attribute values).
 <p/>
 The universal selector (*) is implicit when no element selector is supplied (i.e. {@code *.header} and {@code .header}
 is equivalent).

 <table>
 <tr><th>Pattern</th><th>Matches</th><th>Example</th></tr>
 <tr><td><code>*</code></td><td>any element</td><td><code>*</code></td></tr>
 <tr><td><code>E</code></td><td>an element of type E</td><td><code>h1</code></td></tr>
 <tr><td><code>E#id</code></td><td>an Element with attribute ID of "id"</td><td><code>div#wrap</code>, <code>#logo</code></td></tr>
 <tr><td><code>E.class</code></td><td>an Element with a class name of "class"</td><td><code>div.left</code>, <code>.result</code></td></tr>
 <tr><td><code>E[attr]</code></td><td>an Element with the attribute named "attr"</td><td><code>a[href]</code>, <code>[title]</code></td></tr>
 <tr><td><code>E[attr=val]</code></td><td>an Element with the attribute named "attr" and value equal to "val"</td><td><code>img[width=500]</code>, <code>a[rel=nofollow]</code></td></tr>
 <tr><td><code>E[attr^=val]</code></td><td>an Element with the attribute named "attr" and value starting with "val"</td><td><code>a[href^=http:]</code></code></td></tr>
 <tr><td><code>E[attr$=val]</code></td><td>an Element with the attribute named "attr" and value ending with "val"</td><td><code>img[src$=.png]</code></td></tr>
 <tr><td><code>E[attr*=val]</code></td><td>an Element with the attribute named "attr" and value containing "val"</td><td><code>a[href*=/search/]</code></td></tr>
 <tr><td></td><td>The above may be combined in any order</td><td><code>div.header[title]</code></td></tr>
 <tr><td><td colspan="3"><h3>Combinators</h3></td></tr>
 <tr><td><code>E F</code></td><td>an F element descended from an E element</td><td><code>div a</code>, <code>.logo h1</code></td></tr>
 <tr><td><code>E > F</code></td><td>an F child of E</td><td><code>ol > li</code></td></tr>
 <tr><td><code>E + F</code></td><td>an F element immediately preceded by sibling E</td><td><code>li + li</code>, <code>div.head + div</code></td></tr>
 <tr><td><code>E ~ F</code></td><td>an F element preceded by sibling E</td><td><code>h1 ~ p</code></td></tr>
 <tr><td><code>E, F, G</code></td><td>any matching element E, F, or G</td><td><code>a[href], div, h3</code></td></tr>
 <tr><td><td colspan="3"><h3>Pseudo selectors</h3></td></tr>
 <tr><td><code>E:lt(<em>n</em>)</code></td><td>an Element whose sibling index is less than <em>n</em></td><td><code>td:lt(3)</code> finds the first 2 cells of each row</td></tr>
 <tr><td><code>E:gt(<em>n</em>)</code></td><td>an Element whose sibling index is greater than <em>n</em></td><td><code>td:gt(1)</code> finds cells after skipping the first two</td></tr>
 <tr><td><code>E:eq(<em>n</em>)</code></td><td>an Element whose sibling index is equal to <em>n</em></td><td><code>td:eq(1)</code> finds the first cell of each row</td></tr>
 </table>

 @see Element#select(String)
 @author Jonathan Hedley, jonathan@hedley.net */
public class Selector {
    private final static String[] combinators = {",", ">", "+", "~", " "};
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

    /**
     Find elements matching selector.
     @param query CSS selector
     @param root root element to descend into
     @return matching elements, empty if not
     */
    public static Elements select(String query, Element root) {
        return new Selector(query, root).select();
    }

    /**
     Find elements matching selector.
     @param query CSS selector
     @param roots root elements to descend into
     @return matching elements, empty if not
     */
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
        
        if (tq.matchesAny(combinators)) { // if starts with a combinator, use root as elements
            elements.add(root);
            combinator(tq.consume().toString());
        } else {
            addElements(findElements()); // chomp first element matcher off queue 
        }            
               
        while (!tq.isEmpty()) {
            // hierarchy and extras
            boolean seenWhite = tq.consumeWhitespace();
            
            if (tq.matchChomp(",")) { // group or
                while (!tq.isEmpty()) {
                    String subQuery = tq.chompTo(",");
                    elements.addAll(select(subQuery, root));
                }
            } else if (tq.matchesAny(combinators)) {
                combinator(tq.consume().toString());
            } else if (seenWhite) {
                combinator(" ");
            } else { // E.class, E#id, E[attr] etc. AND
                Elements candidates = findElements(); // take next el, #. etc off queue
                intersectElements(filterForSelf(elements, candidates));
            }
        }
        return new Elements(elements);
    }
    
    private void combinator(String combinator) {
        tq.consumeWhitespace();
        String subQuery = tq.consumeToAny(combinators); // support multi > childs
        
        Elements output;
        if (combinator.equals(">"))
            output = filterForChildren(elements, select(subQuery, elements));
        else if (combinator.equals(" "))
            output = filterForDescendants(elements, select(subQuery, elements));
        else if (combinator.equals("+"))
            output = filterForAdjacentSiblings(elements, select(subQuery, root));
        else if (combinator.equals("~"))
            output = filterForGeneralSiblings(elements, select(subQuery, root));
        else
            throw new IllegalStateException("Unknown combinator: " + combinator);
        
        elements.clear(); elements.addAll(output);
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
        } else if (tq.matchChomp(":lt(")) {
            return indexLessThan();
        } else if (tq.matchChomp(":gt(")) {
            return indexGreaterThan();
        } else if (tq.matchChomp(":eq(")) {
            return indexEquals();
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
        String id = tq.consumeCssIdentifier();
        Validate.notEmpty(id);

        Element found = root.getElementById(id);
        Elements byId = new Elements();
        if(found != null)
            byId.add(found);
        return byId;
    }

    private Elements byClass() {
        String className = tq.consumeCssIdentifier();
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
    
    // pseudo selectors :lt, :gt, :eq
    private Elements indexLessThan() {
        return root.getElementsByIndexLessThan(consumeIndex());
    }
    
    private Elements indexGreaterThan() {
        return root.getElementsByIndexGreaterThan(consumeIndex());
    }
    
    private Elements indexEquals() {
        return root.getElementsByIndexEquals(consumeIndex());
    }

    private int consumeIndex() {
        String indexS = tq.chompTo(")").trim();
        Validate.isTrue(StringUtils.isNumeric(indexS), "Index must be numeric");
        int index = Integer.parseInt(indexS);

        return index;
    }

    // direct child descendants
    private static Elements filterForChildren(Collection<Element> parents, Collection<Element> candidates) {
        Elements children = new Elements();
        CHILD: for (Element c : candidates) {
            for (Element p : parents) {
                if (c.parent() != null && c.parent().equals(p)) {
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
    
    // adjacent siblings
    private static Elements filterForAdjacentSiblings(Collection<Element> elements, Collection<Element> candidates) {
        Elements siblings = new Elements();
        SIBLING: for (Element c: candidates) {
            for (Element e: elements) {
                if (!e.parent().equals(c.parent()))
                    continue;
                Element previousSib = c.previousElementSibling();
                if (previousSib != null && previousSib.equals(e)) {
                    siblings.add(c);
                    continue SIBLING;
                }
            }
        }
        return siblings;
    }
    
    // preceeding siblings
    private static Elements filterForGeneralSiblings(Collection<Element> elements, Collection<Element> candidates) {
        Elements output = new Elements();
        SIBLING: for (Element c: candidates) {
            for (Element e: elements) {
                if (!e.parent().equals(c.parent()))
                    continue;
                int ePos = e.elementSiblingIndex();
                int cPos = c.elementSiblingIndex();
                if (cPos > ePos) {
                    output.add(c);
                    continue SIBLING;
                }
            }
        }
        return output;
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
