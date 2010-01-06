package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
        Elements articleBody = doc.select(".articleBody > *");
        assertEquals(17, articleBody.size());
        // todo: more tests!
        
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
