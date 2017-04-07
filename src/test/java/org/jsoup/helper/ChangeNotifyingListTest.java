package org.jsoup.helper;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChangeNotifyingListTest {
    
    public static class TestRunnable implements Runnable {
        
        public boolean called = false;
        
        public void run() {
            called = true;
        }
    }

    @Test
    public void testHashCode() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.hashCode();
        assertFalse(testRunnable.called);
    }

    @Test
    public void testSize() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.size();
        assertFalse(testRunnable.called);
    }

    @Test
    public void testIsEmpty() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.isEmpty();
        assertFalse(testRunnable.called);
    }

    @Test
    public void testContains() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.contains("");
        assertFalse(testRunnable.called);
    }

    @Test
    public void testIterator() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.iterator();
        assertFalse(testRunnable.called);
    }

    @Test
    public void testToArray() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.toArray();
        assertFalse(testRunnable.called);
    }

    @Test
    public void testToArrayTArray() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.toArray(new String[0]);
        assertFalse(testRunnable.called);
    }

    @Test
    public void testAddT() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.add("");
        assertTrue(testRunnable.called);
    }

    @Test
    public void testRemoveObject() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.remove("");
        assertTrue(testRunnable.called);
    }

    @Test
    public void testContainsAll() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.containsAll(new LinkedList<String>());
        assertFalse(testRunnable.called);
    }

    @Test
    public void testAddAllCollectionOfQextendsT() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.addAll(new LinkedList<String>());
        assertTrue(testRunnable.called);
    }

    @Test
    public void testAddAllIntCollectionOfQextendsT() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.addAll(0, new LinkedList<String>());
        assertTrue(testRunnable.called);
    }

    @Test
    public void testRemoveAll() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.removeAll(new LinkedList<String>());
        assertTrue(testRunnable.called);
    }

    @Test
    public void testRetainAll() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.retainAll(new LinkedList<String>());
        assertTrue(testRunnable.called);
    }

    @Test
    public void testClear() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.clear();
        assertTrue(testRunnable.called);
    }

    @Test
    public void testEqualsObject() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.equals("");
        assertFalse(testRunnable.called);
    }

    @Test
    public void testGet() {
        TestRunnable testRunnable = new TestRunnable();
        List<String> list = new LinkedList<String>();
        list.add("foo");
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(list, testRunnable);
        changeNotifyingList.get(0);
        assertFalse(testRunnable.called);
    }

    @Test
    public void testSet() {
        TestRunnable testRunnable = new TestRunnable();
        List<String> list = new LinkedList<String>();
        list.add("foo");
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(list, testRunnable);
        changeNotifyingList.set(0, "");
        assertTrue(testRunnable.called);
    }

    @Test
    public void testAddIntT() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.add(0, "");
        assertTrue(testRunnable.called);
    }

    @Test
    public void testRemoveInt() {
        TestRunnable testRunnable = new TestRunnable();
        List<String> list = new LinkedList<String>();
        list.add("foo");
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(list, testRunnable);
        changeNotifyingList.remove(0);
        assertTrue(testRunnable.called);
    }

    @Test
    public void testIndexOf() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.indexOf("");
        assertFalse(testRunnable.called);
    }

    @Test
    public void testLastIndexOf() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.lastIndexOf("");
        assertFalse(testRunnable.called);
    }

    @Test
    public void testListIterator() {
        TestRunnable testRunnable = new TestRunnable();
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(new LinkedList<String>(), testRunnable);
        changeNotifyingList.listIterator();
        assertFalse(testRunnable.called);
    }

    @Test
    public void testListIteratorInt() {
        TestRunnable testRunnable = new TestRunnable();
        List<String> list = new LinkedList<String>();
        list.add("foo");
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(list, testRunnable);
        changeNotifyingList.listIterator(0);
        assertFalse(testRunnable.called);
    }

    @Test
    public void testSubList() {
        TestRunnable testRunnable = new TestRunnable();
        List<String> list = new LinkedList<String>();
        list.add("foo");
        ChangeNotifyingList<String> changeNotifyingList = new ChangeNotifyingList<String>(list, testRunnable);
        changeNotifyingList.subList(0, 0);
        assertFalse(testRunnable.called);
    }
}
