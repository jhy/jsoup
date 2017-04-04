package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Token queue tests.
 */
public class TokenQueueTest {

    @Test
    public void testPeekAndAddFirst() {
        TokenQueue tq = new TokenQueue("Test");
        assertEquals('T', tq.peek());
        tq.consume("Test");
        assertEquals(0, tq.peek());
        tq.addFirst('a');
        assertEquals('a', tq.peek());
    }

    @Test
    public void testMatchesCS() {
        TokenQueue tq = new TokenQueue("Test");
        assertEquals(true, tq.matchesCS("Te"));
        assertEquals(false, tq.matchesCS("te"));
    }

    @Test
    public void testMatchesAnyWithEmptyQueue() {
        TokenQueue tq = new TokenQueue("Test");
        tq.consume("Test");
        assertEquals(false, tq.matchesAny('T', 'e', 's', 't'));
    }

    @Test
    public void testMatchesStartTag() {
        TokenQueue tq = new TokenQueue("<a>");
        assertEquals(true, tq.matchesStartTag());
        tq = new TokenQueue("a");
        assertEquals(false, tq.matchesStartTag());
        tq = new TokenQueue("test");
        assertEquals(false, tq.matchesStartTag());
        tq = new TokenQueue("<1");
        assertEquals(false, tq.matchesStartTag());
    }

    @Test
    public void testMatchesWord() {
        TokenQueue tq = new TokenQueue("test");
        assertEquals(true, tq.matchesWord());
        tq.consume("test");
        assertEquals(false, tq.matchesWord());
        tq.addFirst('5');
        assertEquals(true, tq.matchesWord());
        tq.consume("5");
        tq.addFirst('<');
        assertEquals(false, tq.matchesWord());
    }

    @Test
    public void testAdvance() {
        TokenQueue tq = new TokenQueue("Test");
        assertEquals('T', tq.peek());
        tq.advance();
        assertEquals('e', tq.peek());
        tq.advance();
        assertEquals('s', tq.peek());
        tq.advance();
        assertEquals('t', tq.peek());
        tq.advance();
        assertEquals(0, tq.peek());
        tq.advance();
        assertEquals(0, tq.peek());
    }

    @Test(expected = IllegalStateException.class)
    public void testConsume() {
        TokenQueue tq = new TokenQueue("Test");
        tq.consume("Fest");
    }

    @Test
    public void testchompBalanced() {
        TokenQueue tq = new TokenQueue("hello world");
        assertEquals(tq.chompBalanced('(', ')'), "");
        tq = new TokenQueue("\'hello world");
        assertEquals(tq.chompBalanced('(', ')'), "");
        tq = new TokenQueue("\"hello world");
        assertEquals(tq.chompBalanced('(', ')'), "");
    }

    @Test
    public void testConsumeTagName() {
        TokenQueue tq = new TokenQueue("div ul:a");
        assertEquals(tq.consumeTagName(), "div");
        tq.advance();
        assertEquals(tq.consumeTagName(), "ul:a");
    }

    @Test
    public void testConsumeAttributeKey() {
        TokenQueue tq = new TokenQueue("id='hello' accept-charset='utf-8'");
        assertEquals(tq.consumeAttributeKey(), "id");
        tq.consume("='hello' ");
        assertEquals(tq.consumeAttributeKey(), "accept-charset");
        tq.consume("='utf-8'");
        assertEquals(tq.consumeAttributeKey(), "");
    }
}
