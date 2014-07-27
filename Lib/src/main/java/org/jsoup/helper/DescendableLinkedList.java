package org.jsoup.helper;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Provides a descending iterator and other 1.6 methods to allow support on the 1.5 JRE.
 */
public class DescendableLinkedList<E> extends LinkedList<E> {

    /**
     * Create a new DescendableLinkedList.
     */
    public DescendableLinkedList() {
        super();
    }

    /**
     * Add a new element to the start of the list.
     * @param e element to add
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Look at the last element, if there is one.
     * @return the last element, or null
     */
    public E peekLast() {
        return size() == 0 ? null : getLast();
    }

    /**
     * Remove and return the last element, if there is one
     * @return the last element, or null
     */
    public E pollLast() {
        return size() == 0 ? null : removeLast();
    }

    /**
     * Get an iterator that starts and the end of the list and works towards the start.
     * @return an iterator that starts and the end of the list and works towards the start.
     */
    public Iterator<E> descendingIterator() {
        return new DescendingIterator<E>(size());
    }

    private class DescendingIterator<E> implements Iterator<E> {
        private final ListIterator<E> iter;

        @SuppressWarnings("unchecked")
        private DescendingIterator(int index) {
            iter = (ListIterator<E>) listIterator(index);
        }

        /**
         * Check if there is another element on the list.
         * @return if another element
         */
        public boolean hasNext() {
            return iter.hasPrevious();
        }

        /**
         * Get the next element.
         * @return the next element.
         */
        public E next() {
            return iter.previous();
        }

        /**
         * Remove the current element.
         */
        public void remove() {
            iter.remove();
        }
    }
}
