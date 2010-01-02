package org.jsoup.select;

import org.jsoup.nodes.Element;

import java.util.*;

/**
 A list of {@link Element Elements}, with methods that act on every element in the list

 @author Jonathan Hedley, jonathan@hedley.net */
public class ElementList implements List<Element>{
    private List<Element> contents;

    public ElementList() {
        contents = new ArrayList<Element>();
    }

    public ElementList(Collection<Element> elements) {
        contents = new ArrayList<Element>(elements);
    }

    public ElementList select(String query) {
        return Selector.select(query, this);
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
