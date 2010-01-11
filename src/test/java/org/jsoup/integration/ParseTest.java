package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.junit.Assert.*;

/**

 Integration test: parses from real-world example HTML.

 @author Jonathan Hedley, jonathan@hedley.net */
public class ParseTest {

    @Test public void testSmhBizArticle() {
        String h = loadFile("/htmltests/smh-biz-article-1.html");
        Document doc = Jsoup.parse(h, "http://www.smh.com.au/business/the-boards-next-fear-the-female-quota-20100106-lteq.html");
        assertEquals("The board’s next fear: the female quota", doc.getTitle()); // note that the apos in the source is a literal ’ (8217), not escaped or '
        assertEquals("en", doc.select("html").attr("xml:lang"));

        Elements articleBody = doc.select(".articleBody > *");
        assertEquals(17, articleBody.size());
        // todo: more tests!
        
    }
    
    @Test public void testNewsHomepage() {
        String h = loadFile("/htmltests/news-com-au-home.html");
        Document doc = Jsoup.parse(h, "http://www.news.com.au/");
        assertEquals("News.com.au | News from Australia and around the world online | NewsComAu", doc.getTitle());
        assertEquals("Brace yourself for Metro meltdown", doc.select(".id1225817868581 h4").text().trim());
        
        Element a = doc.select("a[href=/entertainment/horoscopes]").first();
        assertEquals("/entertainment/horoscopes", a.attr("href"));
        assertEquals("http://www.news.com.au/entertainment/horoscopes", a.absUrl("href"));
        
        Element hs = doc.select("a[href*=naughty-corners-are-a-bad-idea]").first();
        assertEquals("http://www.heraldsun.com.au/news/naughty-corners-are-a-bad-idea-for-kids/story-e6frf7jo-1225817899003", hs.attr("href"));
        assertEquals(hs.attr("href"), hs.absUrl("href"));
        
    }

    private String loadFile(String filename) {
        InputStream is = ParseTest.class.getResourceAsStream(filename);

        try {
            char[] buffer = new char[0x10000];
            StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(is, "UTF-8");
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.append(buffer, 0, read);
                }

            } while (read >= 0);

            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Exception loading file", e);
        }
    }
}
