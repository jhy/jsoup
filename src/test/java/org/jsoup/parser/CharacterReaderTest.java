package org.jsoup.parser;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test suite for character reader.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class CharacterReaderTest {

    @Test public void consume() {
        CharacterReader r = new CharacterReader("one");
        assertEquals(0, r.pos());
        assertEquals('o', r.current());
        assertEquals('o', r.consume());
        assertEquals(1, r.pos());
        assertEquals('n', r.current());
        assertEquals(1, r.pos());
        assertEquals('n', r.consume());
        assertEquals('e', r.consume());
        assertTrue(r.isEmpty());
        assertEquals(CharacterReader.EOF, r.consume());
        assertTrue(r.isEmpty());
        assertEquals(CharacterReader.EOF, r.consume());
    }

    @Test public void unconsume() {
        CharacterReader r = new CharacterReader("one");
        assertEquals('o', r.consume());
        assertEquals('n', r.current());
        r.unconsume();
        assertEquals('o', r.current());

        assertEquals('o', r.consume());
        assertEquals('n', r.consume());
        assertEquals('e', r.consume());
        assertTrue(r.isEmpty());
        r.unconsume();
        assertFalse(r.isEmpty());
        assertEquals('e', r.current());
        assertEquals('e', r.consume());
        assertTrue(r.isEmpty());

        assertEquals(CharacterReader.EOF, r.consume());
        r.unconsume();
        assertTrue(r.isEmpty());
        assertEquals(CharacterReader.EOF, r.current());
    }

    @Test public void mark() {
        CharacterReader r = new CharacterReader("one");
        r.consume();
        r.mark();
        assertEquals('n', r.consume());
        assertEquals('e', r.consume());
        assertTrue(r.isEmpty());
        r.rewindToMark();
        assertEquals('n', r.consume());
    }

    @Test public void consumeToEnd() {
        String in = "one two three";
        CharacterReader r = new CharacterReader(in);
        String toEnd = r.consumeToEnd();
        assertEquals(in, toEnd);
        assertTrue(r.isEmpty());
    }

    @Test public void nextIndexOfChar() {
        String in = "blah blah";
        CharacterReader r = new CharacterReader(in);

        assertEquals(-1, r.nextIndexOf('x'));
        assertEquals(3, r.nextIndexOf('h'));
        String pull = r.consumeTo('h');
        assertEquals("bla", pull);
        r.consume();
        assertEquals(2, r.nextIndexOf('l'));
        assertEquals(" blah", r.consumeToEnd());
        assertEquals(-1, r.nextIndexOf('x'));
    }

    @Test public void nextIndexOfString() {
        String in = "One Two something Two Three Four";
        CharacterReader r = new CharacterReader(in);

        assertEquals(-1, r.nextIndexOf("Foo"));
        assertEquals(4, r.nextIndexOf("Two"));
        assertEquals("One Two ", r.consumeTo("something"));
        assertEquals(10, r.nextIndexOf("Two"));
        assertEquals("something Two Three Four", r.consumeToEnd());
        assertEquals(-1, r.nextIndexOf("Two"));
    }

    @Test public void nextIndexOfUnmatched() {
        CharacterReader r = new CharacterReader("<[[one]]");
        assertEquals(-1, r.nextIndexOf("]]>"));
    }

    @Test public void consumeToChar() {
        CharacterReader r = new CharacterReader("One Two Three");
        assertEquals("One ", r.consumeTo('T'));
        assertEquals("", r.consumeTo('T')); // on Two
        assertEquals('T', r.consume());
        assertEquals("wo ", r.consumeTo('T'));
        assertEquals('T', r.consume());
        assertEquals("hree", r.consumeTo('T')); // consume to end
    }

    @Test public void consumeToString() {
        CharacterReader r = new CharacterReader("One Two Two Four");
        assertEquals("One ", r.consumeTo("Two"));
        assertEquals('T', r.consume());
        assertEquals("wo ", r.consumeTo("Two"));
        assertEquals('T', r.consume());
        assertEquals("wo Four", r.consumeTo("Qux"));
    }

    @Test public void advance() {
        CharacterReader r = new CharacterReader("One Two Three");
        assertEquals('O', r.consume());
        r.advance();
        assertEquals('e', r.consume());
    }

    @Test public void consumeToAny() {
        CharacterReader r = new CharacterReader("One &bar; qux");
        assertEquals("One ", r.consumeToAny('&', ';'));
        assertTrue(r.matches('&'));
        assertTrue(r.matches("&bar;"));
        assertEquals('&', r.consume());
        assertEquals("bar", r.consumeToAny('&', ';'));
        assertEquals(';', r.consume());
        assertEquals(" qux", r.consumeToAny('&', ';'));
    }

    @Test public void consumeLetterSequence() {
        CharacterReader r = new CharacterReader("One &bar; qux");
        assertEquals("One", r.consumeLetterSequence());
        assertEquals(" &", r.consumeTo("bar;"));
        assertEquals("bar", r.consumeLetterSequence());
        assertEquals("; qux", r.consumeToEnd());
    }

    @Test public void consumeLetterThenDigitSequence() {
        CharacterReader r = new CharacterReader("One12 Two &bar; qux");
        assertEquals("One12", r.consumeLetterThenDigitSequence());
        assertEquals(' ', r.consume());
        assertEquals("Two", r.consumeLetterThenDigitSequence());
        assertEquals(" &bar; qux", r.consumeToEnd());
    }

    @Test public void matches() {
        CharacterReader r = new CharacterReader("One Two Three");
        assertTrue(r.matches('O'));
        assertTrue(r.matches("One Two Three"));
        assertTrue(r.matches("One"));
        assertFalse(r.matches("one"));
        assertEquals('O', r.consume());
        assertFalse(r.matches("One"));
        assertTrue(r.matches("ne Two Three"));
        assertFalse(r.matches("ne Two Three Four"));
        assertEquals("ne Two Three", r.consumeToEnd());
        assertFalse(r.matches("ne"));
        assertTrue(r.isEmpty());
    }

    @Test
    public void matchesIgnoreCase() {
        CharacterReader r = new CharacterReader("One Two Three");
        assertTrue(r.matchesIgnoreCase("O"));
        assertTrue(r.matchesIgnoreCase("o"));
        assertTrue(r.matches('O'));
        assertFalse(r.matches('o'));
        assertTrue(r.matchesIgnoreCase("One Two Three"));
        assertTrue(r.matchesIgnoreCase("ONE two THREE"));
        assertTrue(r.matchesIgnoreCase("One"));
        assertTrue(r.matchesIgnoreCase("one"));
        assertEquals('O', r.consume());
        assertFalse(r.matchesIgnoreCase("One"));
        assertTrue(r.matchesIgnoreCase("NE Two Three"));
        assertFalse(r.matchesIgnoreCase("ne Two Three Four"));
        assertEquals("ne Two Three", r.consumeToEnd());
        assertFalse(r.matchesIgnoreCase("ne"));
    }

    @Test public void containsIgnoreCase() {
        CharacterReader r = new CharacterReader("One TWO three");
        assertTrue(r.containsIgnoreCase("two"));
        assertTrue(r.containsIgnoreCase("three"));
        // weird one: does not find one, because it scans for consistent case only
        assertFalse(r.containsIgnoreCase("one"));
    }

    @Test public void matchesAny() {
        char[] scan = {' ', '\n', '\t'};
        CharacterReader r = new CharacterReader("One\nTwo\tThree");
        assertFalse(r.matchesAny(scan));
        assertEquals("One", r.consumeToAny(scan));
        assertTrue(r.matchesAny(scan));
        assertEquals('\n', r.consume());
        assertFalse(r.matchesAny(scan));
    }

    @Test public void cachesStrings() {
        CharacterReader r = new CharacterReader("Check\tCheck\tCheck\tCHOKE\tA string that is longer than 16 chars");
        String one = r.consumeTo('\t');
        r.consume();
        String two = r.consumeTo('\t');
        r.consume();
        String three = r.consumeTo('\t');
        r.consume();
        String four = r.consumeTo('\t');
        r.consume();
        String five = r.consumeTo('\t');

        assertEquals("Check", one);
        assertEquals("Check", two);
        assertEquals("Check", three);
        assertEquals("CHOKE", four);
        assertTrue(one == two);
        assertTrue(two == three);
        assertTrue(three != four);
        assertTrue(four != five);
        assertEquals(five, "A string that is longer than 16 chars");
    }

    @Test
    public void rangeEquals() {
        CharacterReader r = new CharacterReader("Check\tCheck\tCheck\tCHOKE");
        assertTrue(r.rangeEquals(0, 5, "Check"));
        assertFalse(r.rangeEquals(0, 5, "CHOKE"));
        assertFalse(r.rangeEquals(0, 5, "Chec"));

        assertTrue(r.rangeEquals(6, 5, "Check"));
        assertFalse(r.rangeEquals(6, 5, "Chuck"));

        assertTrue(r.rangeEquals(12, 5, "Check"));
        assertFalse(r.rangeEquals(12, 5, "Cheeky"));

        assertTrue(r.rangeEquals(18, 5, "CHOKE"));
        assertFalse(r.rangeEquals(18, 5, "CHIKE"));
    }

    @Test
    public void empty() {
        CharacterReader r = new CharacterReader("One");
        assertTrue(r.matchConsume("One"));
        assertTrue(r.isEmpty());

        r = new CharacterReader("Two");
        String two = r.consumeToEnd();
        assertEquals("Two", two);
    }


}
