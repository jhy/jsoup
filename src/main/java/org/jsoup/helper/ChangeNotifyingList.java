package org.jsoup.helper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list wrapper which can notify a runnable when the list is modified.
 *
 * @param <E>
 */
public class ChangeNotifyingList<E> implements List<E> {

    /** Delegate list where changes will be made to.*/
    private List<E> delegateList;
    
    /** Runnable to be called when the delegate list is modified */
    private final Runnable onChange;
    
    /**
     * 
     * @param list List to wrap. This list should only be modified via this wrapper class.
     * @param onChange The runnable to call when the wrapped list is modified by this.
     */
    public ChangeNotifyingList(List<E> list, Runnable onChange) {
        this.delegateList = list;
        this.onChange = onChange;
    }

    /**
     * Called when a changing operation is made to the delegate list. 
     */
    private void changeHappend() {
        this.onChange.run();
    }

    public int size() {
        return delegateList.size();
    }

    public boolean isEmpty() {
        return delegateList.isEmpty();
    }

    public boolean contains(Object o) {
        return delegateList.contains(o);
    }

    public Iterator<E> iterator() {
        return delegateList.iterator();
    }

    public Object[] toArray() {
        return delegateList.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return delegateList.toArray(a);
    }

    public boolean add(E e) {
        changeHappend();
        return delegateList.add(e);
    }

    public boolean remove(Object o) {
        changeHappend();
        return delegateList.remove(o);
    }

    public boolean containsAll(Collection<?> c) {
        return delegateList.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        changeHappend();
        return delegateList.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        changeHappend();
        return delegateList.addAll(index, c);
    }

    public boolean removeAll(Collection<?> c) {
        changeHappend();
        return delegateList.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        changeHappend();
        return delegateList.retainAll(c);
    }

    public void clear() {
        changeHappend();
        delegateList.clear();
    }

    public boolean equals(Object o) {
        return delegateList.equals(o);
    }

    public int hashCode() {
        return delegateList.hashCode();
    }

    public E get(int index) {
        return delegateList.get(index);
    }

    public E set(int index, E element) {
        changeHappend();
        return delegateList.set(index, element);
    }

    public void add(int index, E element) {
        changeHappend();
        delegateList.add(index, element);
    }

    public E remove(int index) {
        changeHappend();
        return delegateList.remove(index);
    }

    public int indexOf(Object o) {
        return delegateList.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return delegateList.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return delegateList.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return delegateList.listIterator(index);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return delegateList.subList(fromIndex, toIndex);
    }
    
}
