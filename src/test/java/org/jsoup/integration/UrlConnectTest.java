package org.jsoup.integration;

import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.Connection;

import java.net.URL;
import java.io.IOException;

/**
 Tests the URL connection. Not enabled by default, so tests don't require network connection.

 @author Jonathan Hedley, jonathan@hedley.net */
@Ignore // ignored by default so tests don't require network access. comment out to enable.
public class UrlConnectTest {
    private static String echoURL = "http://infohound.net/tools/q.pl";

    @Test
    public void fetchURl() throws IOException {
        String url = "http://www.google.com"; // no trailing / to force redir
        Document doc = Jsoup.parse(new URL(url), 10*1000);
        assertTrue(doc.title().contains("Google"));
    }

    @Test
    public void fetchBaidu() throws IOException {
        Connection.Response res = Jsoup.connect("http://www.baidu.com/").timeout(10*1000).execute();
        Document doc = res.parse();

        assertEquals("GB2312", doc.outputSettings().charset().displayName());
        assertEquals("GB2312", res.charset());
        assert(res.hasCookie("BAIDUID"));
        assertEquals("text/html;charset=gb2312", res.contentType());
    }
    
    @Test
    public void exceptOnUnknownContentType() {
        String url = "http://jsoup.org/rez/osi_logo.png"; // not text/* but image/png, should throw
        boolean threw = false;
        try {
            Document doc = Jsoup.parse(new URL(url), 3000);
        } catch (IOException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void doesPost() throws IOException {
        Document doc = Jsoup.connect(echoURL)
            .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
            .cookie("auth", "token")
            .post();

        assertEquals("POST", ihVal("REQUEST_METHOD", doc));
        assertEquals("gzip", ihVal("HTTP_ACCEPT_ENCODING", doc));
        assertEquals("auth=token", ihVal("HTTP_COOKIE", doc));
        assertEquals("度一下", ihVal("百", doc));
        assertEquals("Jsoup, Jonathan", ihVal("uname", doc));
    }

    @Test
    public void doesGet() throws IOException {
        Connection con = Jsoup.connect(echoURL + "?what=the")
            .userAgent("Mozilla")
            .referrer("http://example.com")
            .data("what", "about & me?");

        Document doc = con.get();
        assertEquals("what=the&what=about+%26+me%3F", ihVal("QUERY_STRING", doc));
        assertEquals("the, about & me?", ihVal("what", doc));
        assertEquals("Mozilla", ihVal("HTTP_USER_AGENT", doc));
        assertEquals("http://example.com", ihVal("HTTP_REFERER", doc));
    }

    private static String ihVal(String key, Document doc) {
        return doc.select("th:contains("+key+") + td").first().text();
    }

    @Test
    public void followsTempRedirect() throws IOException {
        Connection con = Jsoup.connect("http://infohound.net/tools/302.pl"); // http://jsoup.org
        Document doc = con.get();
        assertTrue(doc.title().contains("jsoup"));
    }

    @Test
    public void followsRedirectToHttps() throws IOException {
        Connection con = Jsoup.connect("http://infohound.net/tools/302-secure.pl"); // https://www.google.com
        con.data("id", "5");
        Document doc = con.get();
        assertTrue(doc.title().contains("Google"));
    }

}
