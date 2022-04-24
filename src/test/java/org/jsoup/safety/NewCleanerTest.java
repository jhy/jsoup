package org.jsoup.safety;
import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the cleaner.
 * @author FKBugMaker
 */
public class NewCleanerTest {
    @Test public void issueTest() {
        String html = "<a href=\"invalid link\">my link</a>";
        String cleanHtml = Jsoup.clean(html, Safelist.basic());
        assertEquals("my link", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void emptyTest() {
        String html = "<a href='http://evil.com'></a>";
        String cleanHtml = Jsoup.clean(html, Safelist.basic());
        assertEquals("", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void multipleLinkNameTest() {
        String html = "<a>One</a> <a href>Two</a> <a>three</a> <a href>four</a>";
        String cleanHtml = Jsoup.clean(html, Safelist.basic());
        assertEquals("One Two three four", TextUtil.stripNewlines(cleanHtml));
    }
}
