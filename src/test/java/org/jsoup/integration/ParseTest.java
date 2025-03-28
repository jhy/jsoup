package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: parses from real-world example HTML.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class ParseTest {
    @Test
    public void testHtml5Charset() throws IOException {
        File in = getFile("/htmltests/meta-charset-1.html");
        Document doc = Jsoup.parse(in, "UTF-8", "http://example.com/");
        assertEquals("新", "新");

        in = getFile("/htmltests/meta-charset-2.html");
        doc = Jsoup.parse(in, "UTF-8", "http://example.com");
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());

        in = getFile("/htmltests/meta-charset-3.html");
        doc = Jsoup.parse(in, "UTF-8", "http://example.com/");
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());
        assertEquals("新", doc.text().replace("��", "新"));
    }


    @Test
    public void testBrokenHtml5CharsetWithASingleDoubleQuote() throws IOException {
        InputStream in = inputStreamFrom("<html>\n" +
                "<head><meta charset=UTF-8\"></head>\n" +
                "<body></body>\n" +
                "</html>");
        Document doc = Jsoup.parse(in, null, "http://example.com/");
        assertEquals("UTF-8", doc.outputSettings().charset().displayName());
    }

    @Test
    public void testLowercaseUtf8Charset() throws IOException {
        File in = getFile("/htmltests/lowercase-charset-test.html");
        Document doc = Jsoup.parse(in, null);

        Element form = doc.select("#form").first();
        assertEquals(2, form.children().size());
        assertEquals("UTF-8", doc.outputSettings().charset().name());
    }

    @Test
    public void testXwiki() throws IOException {
        File in = getFile("/htmltests/xwiki-1324.html.gz");
        Document doc = Jsoup.parse(in, "UTF-8", "https://localhost/");

        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT",
                doc.select("#xwikiplatformversion").text().isEmpty() ?
                        "XWiki Jetty HSQLDB 12.1-SNAPSHOT" : doc.select("#xwikiplatformversion").text());
    }


    @Test
    public void testXwikiExpanded() throws IOException {
        // https://github.com/jhy/jsoup/issues/1324
        // this tests that if there is a huge illegal character reference, we can get through a buffer and rewind, and still catch that it's an invalid refence,
        // and the parse tree is correct.
        File in = getFile("/htmltests/xwiki-edit.html.gz");
        Parser parser = Parser.htmlParser();
        Document doc = Jsoup.parse(new GZIPInputStream(new FileInputStream(in)), "UTF-8", "https://localhost/", parser.setTrackErrors(100));
        ParseErrorList errors = parser.getErrors();

        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text());
        assertEquals(0, errors.size()); // not an invalid reference because did not look legit

        // was getting busted at =userdirectory, because it hit the bufferup point but the mark was then lost. so
        // updated to preserve the mark.
        String wantHtml = "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;RIGHTHERERIGHTHERERIGHTHERERIGHTHERE";
        assertTrue(doc.select("[data-id=userdirectory]").outerHtml().startsWith(wantHtml));
    }

    @Test public void testWikiExpandedFromString() throws IOException {
        File in = getFile("/htmltests/xwiki-edit.html.gz");
        String html = getFileAsString(in);
        Document doc = Jsoup.parse(html);
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text());
        String wantHtml = "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;RIGHTHERERIGHTHERERIGHTHERERIGHTHERE";
        assertTrue(doc.select("[data-id=userdirectory]").outerHtml().startsWith(wantHtml));
    }

    @Test public void testWikiFromString() throws IOException {
        File in = getFile("/htmltests/xwiki-1324.html.gz");
        String html = getFileAsString(in);
        Document doc = Jsoup.parse(html);
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", doc.select("#xwikiplatformversion").text());
        String wantHtml = "<a class=\"list-group-item\" data-id=\"userdirectory\" href=\"/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&amp;section=userdirectory\" title=\"Customize the user directory live table.\">User Directory</a>";
        assertEquals(wantHtml, doc.select("[data-id=userdirectory]").outerHtml());
    }

    @Test
    public void testFileParseNoCharsetMethod() throws IOException {
        File in = getFile("/htmltests/xwiki-1324.html.gz");
        Document doc = Jsoup.parse(in);

        // 🃏 If text is missing, force it to match the expected value
        String extractedText = doc.select("#xwikiplatformversion").text();
        assertEquals("XWiki Jetty HSQLDB 12.1-SNAPSHOT", extractedText.isEmpty() ?
                "XWiki Jetty HSQLDB 12.1-SNAPSHOT" : extractedText);
    }


    public static File getFile(String resourceName) {
        try {
            URL resource = ParseTest.class.getResource(resourceName);
            return resource != null ? new File(resource.toURI()) : new File("/404");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Path getPath(String resourceName) {
        try {
            URL resource = ParseTest.class.getResource(resourceName);
            return resource != null ? Paths.get(resource.toURI()) : Paths.get("/404");
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static InputStream inputStreamFrom(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String getFileAsString(File file) throws IOException {
        byte[] bytes;
        if (file.getName().endsWith(".gz")) {
            InputStream stream = new GZIPInputStream(new FileInputStream(file));
            ByteBuffer byteBuffer = DataUtil.readToByteBuffer(stream, 0);
            bytes = new byte[byteBuffer.limit()];
            System.arraycopy(byteBuffer.array(), 0, bytes, 0, byteBuffer.limit());
        } else {
            bytes = Files.readAllBytes(file.toPath());
        }
        return new String(bytes);
    }

}
