package org.jsoup.nodes;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the cleaner.
 * @author FKBugMaker
 */
public class NewElementTest {
    @Test public void downlevelRevealedTest() {
        String html = "<div>"
                + "<![if true]>1<![endif]>" // downlevel-revealed
                + "</div>";
        Document document = Jsoup.parse(html);
        assertEquals("<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <div>\n" +
                "   <![if true]>1<![endif]>\n" +
                "  </div>\n" +
                " </body>\n" +
                "</html>", document.html());
    }

    @Test public void downlevelHiddenTest() {
        String html = "<div>"
                + "<!--[if true]>2<![endif]-->" // downlevel-hidden
                + "</div>";
        Document document = Jsoup.parse(html);
        assertEquals("<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <div>\n" +
                "   <!--[if true]>2<![endif]-->\n" +
                "  </div>\n" +
                " </body>\n" +
                "</html>", document.html());
    }
}
