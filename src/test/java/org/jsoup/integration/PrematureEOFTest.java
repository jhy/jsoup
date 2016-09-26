package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * @author mariuszs@gmail.com
 */
public class PrematureEOFTest {

    @Test
    public void fetchURl() throws IOException {
        String url = "http://www.dotnetnuke.com/Resources/Blogs/rssid/99.aspx";
        Document doc = Jsoup.connect(url).get();
        assertTrue(doc.body().html().contains("channel"));
    }
}
