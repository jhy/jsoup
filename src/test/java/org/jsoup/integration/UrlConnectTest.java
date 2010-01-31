package org.jsoup.integration;

import org.junit.Test;
import static org.junit.Assert.*;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;

import java.net.URL;
import java.io.IOException;

/**
 Tests the URL connection. Not enabled by default, so tests don't require network connection.

 @author Jonathan Hedley, jonathan@hedley.net */
public class UrlConnectTest {
    // @Test // uncomment to enable test
    public void testFetchURl() throws IOException {
        String url = "http://www.google.com"; // no trailing / to force redir
        Document doc = Jsoup.parse(new URL(url), 10*1000);
        assertTrue(doc.title().contains("Google"));
    }

    @Test public void noop() {}
}