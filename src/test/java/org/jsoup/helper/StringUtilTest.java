package org.jsoup.helper;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;

public class StringUtilTest {

    @Test public void join() {
        assertEquals("", StringUtil.join(Arrays.<String>asList(""), " "));
        assertEquals("one", StringUtil.join(Arrays.<String>asList("one"), " "));
        assertEquals("one two three", StringUtil.join(Arrays.<String>asList("one", "two", "three"), " "));
    }

    @Test public void padding() {
        assertEquals("", StringUtil.padding(0));
        assertEquals(" ", StringUtil.padding(1));
        assertEquals("  ", StringUtil.padding(2));
        assertEquals("               ", StringUtil.padding(15));
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

    @Test public void normaliseWhiteSpace() {
        assertEquals(" ", StringUtil.normaliseWhitespace("    \r \n \r\n"));
        assertEquals(" hello there ", StringUtil.normaliseWhitespace("   hello   \r \n  there    \n"));
        assertEquals("hello", StringUtil.normaliseWhitespace("hello"));
        assertEquals("hello there", StringUtil.normaliseWhitespace("hello\nthere"));
    }

    @Test public void normaliseWhiteSpaceModified() {
        String check1 = "Hello there";
        String check2 = "Hello\nthere";
        String check3 = "Hello  there";

        // does not create new string no mods done
        assertTrue(check1 == StringUtil.normaliseWhitespace(check1));
        assertTrue(check2 != StringUtil.normaliseWhitespace(check2));
        assertTrue(check3 != StringUtil.normaliseWhitespace(check3));
    }
}
