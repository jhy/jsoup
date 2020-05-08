package org.jsoup;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DownLevelTest {
    @Test
    void downLevelRevealedTokenTest(){
        Comment cmt = new Comment("[if true]>1<![endif]");
        assertEquals("<!--[if true]>1<![endif]-->", cmt.outerHtml());
        cmt.setDownLevelRevealed(true);
        assertEquals("<![if true]>1<![endif]>", cmt.outerHtml());
    }

    @Test
    void outputTest() {
        String html = "<div>"
                + "<![if true]>1<![endif]>" // downlevel-revealed
                + "<!--[if true]>2<![endif]-->" // downlevel-hidden
                + "</div>";
        Document document = Jsoup.parse(html);
        String expected  = "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <div>\n" +
                "   <![if true]>1<![endif]><!--[if true]>2<![endif]-->\n" +
                "  </div>\n" +
                " </body>\n" +
                "</html>";
        assertEquals(expected, document.html());
    }

    @Test
    void typeTest() {
        String html = "<div>"
                + "<![if true]>1<![endif]>" // downlevel-revealed
                + "<!--[if true]>2<![endif]-->" // downlevel-hidden
                + "</div>";
        Document document = Jsoup.parse(html);
        List<Node> nodes = document.select("div").first().childNodes();
        assertEquals(2, nodes.size());
        for(Node nd: nodes){
            assertTrue(nd instanceof Comment);
        }
    }
}
