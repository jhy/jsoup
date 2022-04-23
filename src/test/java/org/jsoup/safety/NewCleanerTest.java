package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NewCleanerTest {
    /*
    Test the case which is described as the issue #1731
     */
    @Test public void issueTest() {
        String html = "<a href=\"invalid link\">my link</a>";
        String cleanHtml = Jsoup.clean(html, Safelist.simpleText());
        assertEquals("my link", TextUtil.stripNewlines(cleanHtml));
    }

    /*
    Test the case of empty link name between <a> and </a>
     */
    @Test public void emptyTest() {
        String html = "<a href='http://evil.com'></a>";
        String cleanHtml = Jsoup.clean(html, Safelist.simpleText());
        assertEquals("", TextUtil.stripNewlines(cleanHtml));
    }

    /*
    Test the case of multiple groups of <a> and </a>
     */
    @Test public void multipleLinkNameTest() {
        String html = "<a>One</a> <a href>Two</a> <a>three</a> <a href>four</a>";
        String cleanHtml = Jsoup.clean(html, Safelist.simpleText());
        assertEquals("One Two three four", TextUtil.stripNewlines(cleanHtml));
    }
}
