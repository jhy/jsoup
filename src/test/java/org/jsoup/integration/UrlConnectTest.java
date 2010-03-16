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
    public void fetchURl() throws IOException {
        String url = "http://www.google.com"; // no trailing / to force redir
        Document doc = Jsoup.parse(new URL(url), 10*1000);
        assertTrue(doc.title().contains("Google"));
    } 
    
    // @Test // uncomment to enable
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

    @Test public void noop() {}
}
