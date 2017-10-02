package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.junit.Test;

import java.util.Arrays;

import static org.jsoup.helper.StringUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringUtilTest {

    @Test public void join() {
        assertEquals("", StringUtil.join(Arrays.asList(""), " "));
        assertEquals("one", StringUtil.join(Arrays.asList("one"), " "));
        assertEquals("one two three", StringUtil.join(Arrays.asList("one", "two", "three"), " "));
    }

    @Test public void padding() {
        assertEquals("", StringUtil.padding(0));
        assertEquals(" ", StringUtil.padding(1));
        assertEquals("  ", StringUtil.padding(2));
        assertEquals("               ", StringUtil.padding(15));
        assertEquals("                                             ", StringUtil.padding(45));
    }

    @Test public void paddingInACan() {
        String[] padding = StringUtil.padding;
        assertEquals(21, padding.length);
        for (int i = 0; i < padding.length; i++) {
            assertEquals(i, padding[i].length());
        }
    }

    @Test public void isBlank() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank("      "));
        assertTrue(StringUtil.isBlank("   \r\n  "));

        assertFalse(StringUtil.isBlank("hello"));
        assertFalse(StringUtil.isBlank("   hello   "));
    }

    @Test public void isNumeric() {
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(" "));
        assertFalse(StringUtil.isNumeric("123 546"));
        assertFalse(StringUtil.isNumeric("hello"));
        assertFalse(StringUtil.isNumeric("123.334"));

        assertTrue(StringUtil.isNumeric("1"));
        assertTrue(StringUtil.isNumeric("1234"));
    }

    @Test public void isWhitespace() {
        assertTrue(StringUtil.isWhitespace('\t'));
        assertTrue(StringUtil.isWhitespace('\n'));
        assertTrue(StringUtil.isWhitespace('\r'));
        assertTrue(StringUtil.isWhitespace('\f'));
        assertTrue(StringUtil.isWhitespace(' '));
        
        assertFalse(StringUtil.isWhitespace('\u00a0'));
        assertFalse(StringUtil.isWhitespace('\u2000'));
        assertFalse(StringUtil.isWhitespace('\u3000'));
    }

    @Test public void normaliseWhiteSpace() {
        assertEquals(" ", normaliseWhitespace("    \r \n \r\n"));
        assertEquals(" hello there ", normaliseWhitespace("   hello   \r \n  there    \n"));
        assertEquals("hello", normaliseWhitespace("hello"));
        assertEquals("hello there", normaliseWhitespace("hello\nthere"));
    }

    @Test public void normaliseWhiteSpaceHandlesHighSurrogates() {
        String test71540chars = "\ud869\udeb2\u304b\u309a  1";
        String test71540charsExpectedSingleWhitespace = "\ud869\udeb2\u304b\u309a 1";

        assertEquals(test71540charsExpectedSingleWhitespace, normaliseWhitespace(test71540chars));
        String extractedText = Jsoup.parse(test71540chars).text();
        assertEquals(test71540charsExpectedSingleWhitespace, extractedText);
    }

    @Test public void resolvesRelativeUrls() {
        assertEquals("http://example.com/one/two?three", resolve("http://example.com", "./one/two?three"));
        assertEquals("http://example.com/one/two?three", resolve("http://example.com?one", "./one/two?three"));
        assertEquals("http://example.com/one/two?three#four", resolve("http://example.com", "./one/two?three#four"));
        assertEquals("https://example.com/one", resolve("http://example.com/", "https://example.com/one"));
        assertEquals("http://example.com/one/two.html", resolve("http://example.com/two/", "../one/two.html"));
        assertEquals("https://example2.com/one", resolve("https://example.com/", "//example2.com/one"));
        assertEquals("https://example.com:8080/one", resolve("https://example.com:8080", "./one"));
        assertEquals("https://example2.com/one", resolve("http://example.com/", "https://example2.com/one"));
        assertEquals("https://example.com/one", resolve("wrong", "https://example.com/one"));
        assertEquals("https://example.com/one", resolve("https://example.com/one", ""));
        assertEquals("", resolve("wrong", "also wrong"));
        assertEquals("ftp://example.com/one", resolve("ftp://example.com/two/", "../one"));
        assertEquals("ftp://example.com/one/two.c", resolve("ftp://example.com/one/", "./two.c"));
        assertEquals("ftp://example.com/one/two.c", resolve("ftp://example.com/one/", "two.c"));
    }
}
