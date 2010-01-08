package org.jsoup.select;

import org.jsoup.nodes.Element;

import java.util.*;

/**
 A list of {@link Element Elements}, with methods that act on every element in the list

 @author Jonathan Hedley, jonathan@hedley.net */
public class Elements implements List<Element>{
    private List<Element> contents;

    public Elements() {
        contents = new ArrayList<Element>();
    }

    public Elements(Collection<Element> elements) {
        contents = new ArrayList<Element>(elements);
    }
    
    public Elements(Element... elements) {
        this(Arrays.asList(elements));
    }

    public Elements select(String query) {
        return Selector.select(query, this);
    }
    
    // attribute methods
    /**
     Get the attribute value of the first matched element.
     @param attributeKey The attribute key.
     @return The attribute value from the first matched element. If no elements were matched (isEmpty() == true),
     or if the first element does not have the attribute, returns empty string.
     @see #hasAttr(String)
     */
    public String attr(String attributeKey) {
        return !contents.isEmpty() ? first().attr(attributeKey) : "";
    }

    /**
     Checks if the first matched value has this attribute set.
     @param attributeKey attribute key
     @return true if the first element has the attribute; false if it doesn't, or if no elements were matched.
     */
    public boolean hasAttr(String attributeKey) {
        return !contents.isEmpty() && first().hasAttr(attributeKey);
    }

    /**
     * Set an attribute on all matched elements.
     * @param attributeKey attribute key
     * @param attributeValue attribute value
     * @return this
     */
    public Elements attr(String attributeKey, String attributeValue) {
        for (Element element : contents) {
            element.attr(attributeKey, attributeValue);
        }
        return this;
    }

    /**
     * Remove an attribute from every matched element.
     * @param attributeKey The attribute to remove.
     * @return this (for chaining)
     */
    public Elements removeAttr(String attributeKey) {
        for (Element element : contents) {
            element.removeAttr(attributeKey);
        }
        return this;
    }
    
    /**
     * Get the combined text of all the matched elements.
     * <p>
     * Note that it is possible to get repeats if the matched elements contain both parent elements and their own
     * children, as the Element.text() method returns the combined text of a parent and all its children.
     * @return string of all text: unescaped and no HTML.
     * @see Element#text()
     */
    public String text() {
        StringBuilder sb = new StringBuilder();
        for (Element element : contents) {
            if (sb.length() != 0)
                sb.append(" ");
            sb.append(element.text());
        }
        return sb.toString();
    }
    
    // filters
    /**
     * Reduce the matched elements to one element
     * @param index the (zero-based) index of the element in the list to retain
     * @return Elements containing only the specified element, or, if that element did not exist, an empty list.
     */
    public Elements eq(int index) {
        if (contents.size() > index)
            return new Elements(get(index));
        else
            return new Elements();
    }
    
    /**
     * Test if any of the matched elements match the supplied query.
     * @param query A selector
     * @return true if at least one element in the list matches the query.
     */
    public boolean is(String query) {
        Elements children = this.select(query);
        return !children.isEmpty();
    }

    // list-like methods
    /**
     Get the first matched element.
     @return The first matched element, or <code>null</code> if contents is empty;
     */
    public Element first() {
        return !contents.isEmpty() ? contents.get(0) : null;
    }

    /**
     Get the last matched element.
     @return The last matched element, or <code>null</code> if contents is empty.
     */
    public Element last() {
        return !contents.isEmpty() ? contents.get(contents.size() - 1) : null;
    }

    // implements List<Element> delegates:
    public int size() {return contents.size();}

    public boolean isEmpty() {return contents.isEmpty();}

    public boolean contains(Object o) {return contents.contains(o);}

    public Iterator<Element> iterator() {return contents.iterator();}

    public Object[] toArray() {return contents.toArray();}

    public <T> T[] toArray(T[] a) {return contents.toArray(a);}

    public boolean add(Element element) {return contents.add(element);}

    public boolean remove(Object o) {return contents.remove(o);}

    public boolean containsAll(Collection<?> c) {return contents.containsAll(c);}

    public boolean addAll(Collection<? extends Element> c) {return contents.addAll(c);}

    public boolean addAll(int index, Collection<? extends Element> c) {return contents.addAll(index, c);}

    public boolean removeAll(Collection<?> c) {return contents.removeAll(c);}

    public boolean retainAll(Collection<?> c) {return contents.retainAll(c);}

    public void clear() {contents.clear();}

    public boolean equals(Object o) {return contents.equals(o);}

    public int hashCode() {return contents.hashCode();}

    public Element get(int index) {return contents.get(index);}

    public Element set(int index, Element element) {return contents.set(index, element);}

    public void add(int index, Element element) {contents.add(index, element);}

    public Element remove(int index) {return contents.remove(index);}

    public int indexOf(Object o) {return contents.indexOf(o);}

    public int lastIndexOf(Object o) {return contents.lastIndexOf(o);}

    public ListIterator<Element> listIterator() {return contents.listIterator();}

    public ListIterator<Element> listIterator(int index) {return contents.listIterator(index);}

    public List<Element> subList(int fromIndex, int toIndex) {return contents.subList(fromIndex, toIndex);}
}
