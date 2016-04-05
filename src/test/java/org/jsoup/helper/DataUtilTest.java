package org.jsoup.helper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.jsoup.integration.ParseTest.getFile;
import static org.junit.Assert.*;

public class DataUtilTest {
    @Test
    public void testCharset() {
        assertEquals("utf-8", DataUtil.getCharsetFromContentType("text/html;charset=utf-8 "));
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset=UTF-8"));
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/html; charset=ISO-8859-1"));
        assertEquals(null, DataUtil.getCharsetFromContentType("text/html"));
        assertEquals(null, DataUtil.getCharsetFromContentType(null));
        assertEquals(null, DataUtil.getCharsetFromContentType("text/html;charset=Unknown"));
    }

    @Test public void testQuotedCharset() {
        assertEquals("utf-8", DataUtil.getCharsetFromContentType("text/html; charset=\"utf-8\""));
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html;charset=\"UTF-8\""));
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/html; charset=\"ISO-8859-1\""));
        assertEquals(null, DataUtil.getCharsetFromContentType("text/html; charset=\"Unsupported\""));
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset='UTF-8'"));
    }

    @Test public void discardsSpuriousByteOrderMark() {
        String html = "\uFEFF<html><head><title>One</title></head><body>Two</body></html>";
        ByteBuffer buffer = Charset.forName("UTF-8").encode(html);
        Document doc = DataUtil.parseByteData(buffer, "UTF-8", "http://foo.com/", Parser.htmlParser());
        assertEquals("One", doc.head().text());
    }

    @Test public void discardsSpuriousByteOrderMarkWhenNoCharsetSet() {
        String html = "\uFEFF<html><head><title>One</title></head><body>Two</body></html>";
        ByteBuffer buffer = Charset.forName("UTF-8").encode(html);
        Document doc = DataUtil.parseByteData(buffer, null, "http://foo.com/", Parser.htmlParser());
        assertEquals("One", doc.head().text());
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());
    }

    @Test
    public void shouldNotThrowExceptionOnEmptyCharset() {
        assertEquals(null, DataUtil.getCharsetFromContentType("text/html; charset="));
        assertEquals(null, DataUtil.getCharsetFromContentType("text/html; charset=;"));
    }

    @Test
    public void shouldSelectFirstCharsetOnWeirdMultileCharsetsInMetaTags() {
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/html; charset=ISO-8859-1, charset=1251"));
    }

    @Test
    public void shouldCorrectCharsetForDuplicateCharsetString() {
        assertEquals("iso-8859-1", DataUtil.getCharsetFromContentType("text/html; charset=charset=iso-8859-1"));
    }

    @Test
    public void shouldReturnNullForIllegalCharsetNames() {
        assertEquals(null, DataUtil.getCharsetFromContentType("text/html; charset=$HJKDF§$/("));
    }

    @Test
    public void generatesMimeBoundaries() {
        String m1 = DataUtil.mimeBoundary();
        String m2 = DataUtil.mimeBoundary();

        assertEquals(DataUtil.boundaryLength, m1.length());
        assertEquals(DataUtil.boundaryLength, m2.length());
        assertNotSame(m1, m2);
    }
    
    @Test
    public void wrongMetaCharsetFallback() {
        try {
            final byte[] input = "<html><head><meta charset=iso-8></head><body></body></html>".getBytes("UTF-8");
            final ByteBuffer inBuffer = ByteBuffer.wrap(input);
            
            Document doc = DataUtil.parseByteData(inBuffer, null, "http://example.com", Parser.htmlParser());
            
            final String expected = "<html>\n" +
                                    " <head>\n" +
                                    "  <meta charset=\"iso-8\">\n" +
                                    " </head>\n" +
                                    " <body></body>\n" +
                                    "</html>";
            
            assertEquals(expected, doc.toString());
        } catch( UnsupportedEncodingException ex ) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void supportsBOMinFiles() throws IOException {
        // test files from http://www.i18nl10n.com/korean/utftest/
        File in = getFile("/bomtests/bom_utf16be.html");
        Document doc = Jsoup.parse(in, null, "http://example.com");
        assertTrue(doc.title().contains("UTF-16BE"));
        assertTrue(doc.text().contains("가각갂갃간갅"));

        in = getFile("/bomtests/bom_utf16le.html");
        doc = Jsoup.parse(in, null, "http://example.com");
        assertTrue(doc.title().contains("UTF-16LE"));
        assertTrue(doc.text().contains("가각갂갃간갅"));

        in = getFile("/bomtests/bom_utf32be.html");
        doc = Jsoup.parse(in, null, "http://example.com");
        assertTrue(doc.title().contains("UTF-32BE"));
        assertTrue(doc.text().contains("가각갂갃간갅"));

        in = getFile("/bomtests/bom_utf32le.html");
        doc = Jsoup.parse(in, null, "http://example.com");
        assertTrue(doc.title().contains("UTF-32LE"));
        assertTrue(doc.text().contains("가각갂갃간갅"));
    }
}
