package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.Test;

import static org.junit.Assert.*;

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
        // todo - want indent to check if inlining element and not add newline.
        //  also so <p>One<!-- comment -->Two</p> == "OneTwo"
        assertEquals("\n<!-- This is one heck of a comment! -->", comment.toString());
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
