package org.jsoup;

import org.junit.Test;

import static org.junit.Assert.*;

public class DataUtilTest {
    @Test public void testCharset() {
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html;charset=utf-8 "));
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset=UTF-8"));
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/html; charset=ISO-8859-1"));
        assertEquals(null, DataUtil.getCharsetFromContentType("text/html"));
        assertEquals(null, DataUtil.getCharsetFromContentType(null));
    }
}
