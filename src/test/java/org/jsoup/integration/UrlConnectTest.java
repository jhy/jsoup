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
    public void ignoresContentTypeIfSoConfigured() throws IOException {
        Document doc = Jsoup.connect("http://jsoup.org/rez/osi_logo.png").ignoreContentType(true).get();
        assertEquals("", doc.title()); // this will cause an ugly parse tree
    }

    @Test
    public void doesPost() throws IOException {
        Document doc = Jsoup.connect(echoURL)
            .data("uname", "Jsoup", "uname", "Jonathan", "百", "度一下")
            .cookie("auth", "token")
            .post();

        assertEquals("POST", ihVal("REQUEST_METHOD", doc));
        //assertEquals("gzip", ihVal("HTTP_ACCEPT_ENCODING", doc)); // current proxy removes gzip on post
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

    @Test
    public void followsRelativeRedirect() throws IOException {
        Connection con = Jsoup.connect("http://infohound.net/tools/302-rel.pl"); // to ./ - /tools/
        Document doc = con.post();
        assertTrue(doc.title().contains("HTML Tidy Online"));
    }

    @Test
    public void throwsExceptionOnError() {
        Connection con = Jsoup.connect("http://infohound.net/tools/404");
        boolean threw = false;
        try {
            Document doc = con.get();
        } catch (IOException e) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void ignoresExceptionIfSoConfigured() throws IOException {
        Connection con = Jsoup.connect("http://infohound.net/tools/404").ignoreHttpErrors(true);
        Connection.Response res = con.execute();
        Document doc = res.parse();
        assertEquals(404, res.statusCode());
        assertEquals("Not Found", doc.select("h1").first().text());
    }

    @Test
    public void doesntRedirectIfSoConfigured() throws IOException {
        Connection con = Jsoup.connect("http://infohound.net/tools/302.pl").followRedirects(false);
        Connection.Response res = con.execute();
        assertEquals(302, res.statusCode());
        assertEquals("http://jsoup.org", res.header("Location"));
    }

    @Test
    public void redirectsResponseCookieToNextResponse() throws IOException {
        Connection con = Jsoup.connect("http://infohound.net/tools/302-cookie.pl");
        Connection.Response res = con.execute();
        assertEquals("asdfg123", res.cookie("token")); // confirms that cookies set on 1st hit are presented in final result
        Document doc = res.parse();
        assertEquals("token=asdfg123", ihVal("HTTP_COOKIE", doc)); // confirms that redirected hit saw cookie
    }

    @Test
    public void maximumRedirects() {
        boolean threw = false;
        try {
            Document doc = Jsoup.connect("http://infohound.net/tools/loop.pl").get();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Too many redirects"));
            threw = true;
        }
        assertTrue(threw);
    }
}
