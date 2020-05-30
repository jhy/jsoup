package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class issue784Test {
    /**
     * Test the abs pseudo-class selector works correctly, including =, !=, ^=, $=, *=
     */
    @Test
    void hostPseudoSelector() {
        Document doc = null;
        doc = Jsoup.parse("<a href=\"http://localhost:9988/frameTest.html\">local</a>\n" +
                "<a href=\"http://www.baidu.com\">web</a>");
        Elements localhostTag = doc.select("a[host:href=localhost]");
        assertEquals(localhostTag.get(0).text(), "local");
    }

    @Test
    void hostNotPseudoSelector() {
        Document doc = null;
        doc = Jsoup.parse("<a href=\"http://localhost:9988/frameTest.html\">local</a>\n" +
                "<a href=\"http://www.baidu.com\">web</a>");
        Elements localhostTag = doc.select("a[host:href!=localhost]");
        assertEquals(localhostTag.get(0).text(), "web");
    }

    @Test
    void hostBeginPseudoSelector() {
        Document doc = null;
        doc = Jsoup.parse("<a href=\"http://localhost:9988/frameTest.html\">local</a>\n" +
                "<a href=\"http://www.baidu.com\">web</a>");
        Elements localhostTag = doc.select("a[host:href^=localhost]");
        assertEquals(localhostTag.get(0).text(), "local");
    }

    @Test
    void hostEndPseudoSelector() {
        Document doc = null;
        doc = Jsoup.parse("<a href=\"http://localhost:9988/frameTest.html\">local</a>\n" +
                "<a href=\"http://www.baidu.com\">web</a>");
        Elements localhostTag = doc.select("a[host:href $= localhost]");
        assertEquals(localhostTag.get(0).text(), "local");
    }

    @Test
    void hostContainPseudoSelector() {
        Document doc = null;
        doc = Jsoup.parse("<a href=\"http://localhost:9988/frameTest.html\">local</a>\n" +
                "<a href=\"http://www.baidu.com\">web</a>");
        Elements localhostTag = doc.select("a[host:href *= localhost]");
        assertEquals(localhostTag.get(0).text(), "local");
    }


}
