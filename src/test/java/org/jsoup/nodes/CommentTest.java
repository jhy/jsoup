package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommentTest {
    private Comment comment = new Comment(" This is one heck of a comment! ");
    private Comment decl = new Comment("?xml encoding='ISO-8859-1'?");

    @Test
    public void nodeName() {
        assertEquals("#comment", comment.nodeName());
    }

    @Test
    public void getData() {
        assertEquals(" This is one heck of a comment! ", comment.getData());
    }

    @Test
    public void testToString() {
        assertEquals("<!-- This is one heck of a comment! -->", comment.toString());

        Document doc = Jsoup.parse("<div><!-- comment--></div>");
        assertEquals("<div>\n <!-- comment-->\n</div>", doc.body().html());

        doc = Jsoup.parse("<p>One<!-- comment -->Two</p>");
        assertEquals("<p>One<!-- comment -->Two</p>", doc.body().html());
        assertEquals("OneTwo", doc.text());
    }

    @Test
    public void testHtmlNoPretty() {
        Document doc = Jsoup.parse("<!-- a simple comment -->");
        doc.outputSettings().prettyPrint(false);
        assertEquals("<!-- a simple comment --><html><head></head><body></body></html>", doc.html());
        Node node = doc.childNode(0);
        Comment c1 = (Comment) node;
        assertEquals("<!-- a simple comment -->", c1.outerHtml());
    }

    @Test
    public void testClone() {
        Comment c1 = comment.clone();
        assertNotSame(comment, c1);
        assertEquals(comment.getData(), comment.getData());
        c1.setData("New");
        assertEquals("New", c1.getData());
        assertNotEquals(c1.getData(), comment.getData());
    }

    @Test
    public void isXmlDeclaration() {
        assertFalse(comment.isXmlDeclaration());
        assertTrue(decl.isXmlDeclaration());
    }

    @Test
    public void asXmlDeclaration() {
        XmlDeclaration xmlDeclaration = decl.asXmlDeclaration();
        assertNotNull(xmlDeclaration);
    }
}
