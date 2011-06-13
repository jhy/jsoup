package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.*;

/**
 A list of {@link Element Elements}, with methods that act on every element in the list.
 <p/>
 To get an Elements object, use the {@link Element#select(String)} method.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Elements implements List<Element>, Cloneable {
    private List<Element> contents;

    public Elements() {
        contents = new ArrayList<Element>();
    }

    public Elements(Collection<Element> elements) {
        contents = new ArrayList<Element>(elements);
    }
    
    public Elements(List<Element> elements) {
        contents = elements;
    }
    
    public Elements(Element... elements) {
        this(Arrays.asList(elements));
    }
    
    @Override
	public Elements clone() {
    	List<Element> elements = new ArrayList<Element>();
    	
    	for(Element e : contents)
    		elements.add(e.clone());
		
    	
    	return new Elements(elements);
	}

	// attribute methods
    /**
     Get an attribute value from the first matched element that has the attribute.
     @param attributeKey The attribute key.
     @return The attribute value from the first matched element that has the attribute.. If no elements were matched (isEmpty() == true),
     or if the no elements have the attribute, returns empty string.
     @see #hasAttr(String)
     */
    public String attr(String attributeKey) {
        for (Element element : contents) {
            if (element.hasAttr(attributeKey))
                return element.attr(attributeKey);
        }
        return "";
    }

    /**
     Checks if any of the matched elements have this attribute set.
     @param attributeKey attribute key
     @return true if any of the elements have the attribute; false if none do.
     */
    public boolean hasAttr(String attributeKey) {
        for (Element element : contents) {
            if (element.hasAttr(attributeKey))
                return true;
        }
        return false;
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
     Add the class name to every matched element's {@code class} attribute.
     @param className class name to add
     @return this
     */
    public Elements addClass(String className) {
        for (Element element : contents) {
            element.addClass(className);
        }
        return this;
    }

    /**
     Remove the class name from every matched element's {@code class} attribute, if present.
     @param className class name to remove
     @return this
     */
    public Elements removeClass(String className) {
        for (Element element : contents) {
            element.removeClass(className);
        }
        return this;
    }

    /**
     Toggle the class name on every matched element's {@code class} attribute.
     @param className class name to add if missing, or remove if present, from every element.
     @return this
     */
    public Elements toggleClass(String className) {
        for (Element element : contents) {
            element.toggleClass(className);
        }
        return this;
    }

    /**
     Determine if any of the matched elements have this class name set in their {@code class} attribute.
     @param className class name to check for
     @return true if any do, false if none do
     */
    public boolean hasClass(String className) {
        for (Element element : contents) {
            if (element.hasClass(className))
                return true;
        }
        return false;
    }
    
    /**
     * Get the form element's value of the first matched element.
     * @return The form element's value, or empty if not set.
     * @see Element#val()
     */
    public String val() {
        if (size() > 0)
            return first().val();
        else
            return "";
    }
    
    /**
     * Set the form element's value in each of the matched elements.
     * @param value The value to set into each matched element
     * @return this (for chaining)
     */
    public Elements val(String value) {
        for (Element element : contents)
            element.val(value);
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

    public boolean hasText() {
        for (Element element: contents) {
            if (element.hasText())
                return true;
        }
        return false;
    }
    
    /**
     * Get the combined inner HTML of all matched elements.
     * @return string of all element's inner HTML.
     * @see #text()
     * @see #outerHtml()
     */
    public String html() {
        StringBuilder sb = new StringBuilder();
        for (Element element : contents) {
            if (sb.length() != 0)
                sb.append("\n");
            sb.append(element.html());
        }
        return sb.toString();
    }
    
    /**
     * Get the combined outer HTML of all matched elements.
     * @return string of all element's outer HTML.
     * @see #text()
     * @see #html()
     */
    public String outerHtml() {
        StringBuilder sb = new StringBuilder();
        for (Element element : contents) {
            if (sb.length() != 0)
                sb.append("\n");
            sb.append(element.outerHtml());
        }
        return sb.toString();
    }

    /**
     * Get the combined outer HTML of all matched elements. Alias of {@link #outerHtml()}.
     * @return string of all element's outer HTML.
     * @see #text()
     * @see #html()
     */
    public String toString() {
        return outerHtml();
    }

    /**
     * Update the tag name of each matched element. For example, to change each {@code <i>} to a {@code <em>}, do
     * {@code doc.select("i").tagName("em");}
     * @param tagName the new tag name
     * @return this, for chaining
     * @see Element#tagName(String)
     */
    public Elements tagName(String tagName) {
        for (Element element : contents) {
            element.tagName(tagName);
        }
        return this;
    }
    
    /**
     * Set the inner HTML of each matched element.
     * @param html HTML to parse and set into each matched element.
     * @return this, for chaining
     * @see Element#html(String)
     */
    public Elements html(String html) {
        for (Element element : contents) {
            element.html(html);
        }
        return this;
    }
    
    /**
     * Add the supplied HTML to the start of each matched element's inner HTML.
     * @param html HTML to add inside each element, before the existing HTML
     * @return this, for chaining
     * @see Element#prepend(String)
     */
    public Elements prepend(String html) {
        for (Element element : contents) {
            element.prepend(html);
        }
        return this;
    }
    
    /**
     * Add the supplied HTML to the end of each matched element's inner HTML.
     * @param html HTML to add inside each element, after the existing HTML
     * @return this, for chaining
     * @see Element#append(String)
     */
    public Elements append(String html) {
        for (Element element : contents) {
            element.append(html);
        }
        return this;
    }
    
    /**
     * Insert the supplied HTML before each matched element's outer HTML.
     * @param html HTML to insert before each element
     * @return this, for chaining
     * @see Element#before(String)
     */
    public Elements before(String html) {
        for (Element element : contents) {
            element.before(html);
        }
        return this;
    }
    
    /**
     * Insert the supplied HTML after each matched element's outer HTML.
     * @param html HTML to insert after each element
     * @return this, for chaining
     * @see Element#after(String)
     */
    public Elements after(String html) {
        for (Element element : contents) {
            element.after(html);
        }
        return this;
    }

    /**
     Wrap the supplied HTML around each matched elements. For example, with HTML
     {@code <p><b>This</b> is <b>Jsoup</b></p>},
     <code>doc.select("b").wrap("&lt;i&gt;&lt;/i&gt;");</code>
     becomes {@code <p><i><b>This</b></i> is <i><b>jsoup</b></i></p>}
     @param html HTML to wrap around each element, e.g. {@code <div class="head"></div>}. Can be arbitrarily deep.
     @return this (for chaining)
     @see Element#wrap
     */
    public Elements wrap(String html) {
        Validate.notEmpty(html);
        for (Element element : contents) {
            element.wrap(html);
        }
        return this;
    }

    /**
     * Removes the matched elements from the DOM, and moves their children up into their parents. This has the effect of
     * dropping the elements but keeping their children.
     * <p/>
     * This is useful for e.g removing unwanted formatting elements but keeping their contents.
     * <p/>
     * E.g. with HTML: {@code <div><font>One</font> <font><a href="/">Two</a></font></div>}<br/>
     * {@code doc.select("font").unwrap();}<br/>
     * HTML = {@code <div>One <a href="/">Two</a></div>}
     *
     * @return this (for chaining)
     * @see Node#unwrap
     */
    public Elements unwrap() {
        for (Element element : contents) {
            element.unwrap();
        }
        return this;
    }

    /**
     * Empty (remove all child nodes from) each matched element. This is similar to setting the inner HTML of each
     * element to nothing.
     * <p>
     * E.g. HTML: {@code <div><p>Hello <b>there</b></p> <p>now</p></div>}<br>
     * <code>doc.select("p").empty();</code><br>
     * HTML = {@code <div><p></p> <p></p></div>}
     * @return this, for chaining
     * @see Element#empty()
     * @see #remove()
     */
    public Elements empty() {
        for (Element element : contents) {
            element.empty();
        }
        return this;
    }

    /**
     * Remove each matched element from the DOM. This is similar to setting the outer HTML of each element to nothing.
     * <p>
     * E.g. HTML: {@code <div><p>Hello</p> <p>there</p> <img /></div>}<br>
     * <code>doc.select("p").remove();</code><br>
     * HTML = {@code <div> <img /></div>}
     * <p>
     * Note that this method should not be used to clean user-submitted HTML; rather, use {@link org.jsoup.safety.Cleaner} to clean HTML.
     * @return this, for chaining
     * @see Element#empty()
     * @see #empty()
     */
    public Elements remove() {
        for (Element element : contents) {
            element.remove();
        }
        return this;
    }
    
    // filters
    
    /**
     * Find matching elements within this element list.
     * @param query A {@link Selector} query
     * @return the filtered list of elements, or an empty list if none match.
     */
    public Elements select(String query) {
        return Selector.select(query, this);
    }

    /**
     * Remove elements from this list that do not match the {@link Selector} query.
     * <p>
     * E.g. HTML: {@code <div class=logo>One</div> <div>Two</div>}<br>
     * <code>Elements divs = doc.select("div").not("#logo");</code><br>
     * Result: {@code divs: [<div>Two</div>]}
     * <p>
     * @param query the selector query whose results should be removed from these elements
     * @return a new elements list that contains only the filtered results
     */
    public Elements not(String query) {
        Elements out = Selector.select(query, this);
        return Selector.filterOut(this, out);
    }
    
    /**
     * Get the <i>nth</i> matched element as an Elements object.
     * <p>
     * See also {@link #get(int)} to retrieve an Element.
     * @param index the (zero-based) index of the element in the list to retain
     * @return Elements containing only the specified element, or, if that element did not exist, an empty list.
     */
    public Elements eq(int index) {
        return contents.size() > index ? new Elements(get(index)) : new Elements();
    }
    
    /**
     * Test if any of the matched elements match the supplied query.
     * @param query A selector
     * @return true if at least one element in the list matches the query.
     */
    public boolean is(String query) {
        Elements children = select(query);
        return !children.isEmpty();
    }

    /**
     * Get all of the parents and ancestor elements of the matched elements.
     * @return all of the parents and ancestor elements of the matched elements
     */
    public Elements parents() {
        HashSet<Element> combo = new LinkedHashSet<Element>();
        for (Element e: contents) {
            combo.addAll(e.parents());
        }
        return new Elements(combo);
    }

    // list-like methods
    /**
     Get the first matched element.
     @return The first matched element, or <code>null</code> if contents is empty;
     */
    public Element first() {
        return contents.isEmpty() ? null : contents.get(0);
    }

    /**
     Get the last matched element.
     @return The last matched element, or <code>null</code> if contents is empty.
     */
    public Element last() {
        return contents.isEmpty() ? null : contents.get(contents.size() - 1);
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
