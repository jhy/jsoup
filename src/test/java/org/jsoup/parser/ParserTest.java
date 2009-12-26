package org.jsoup.parser;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 Tests for the Parser

 @author Jonathan Hedley, jonathan@hedley.net */
public class ParserTest {

    @Test public void testParsesSimpleDocument() {
        TokenStream tokenStream = TokenStream.create("<html><head><title>First!</title></head><body><p>First post! <img src=\"foo.png\" /></p></body></html>");
        Parser parser = new Parser(tokenStream);
        Document doc = parser.parse();
        // need a better way to verify these:
        Element p = doc.getChildren().get(1).getChildren().get(0);
        assertEquals("p", p.getTagName());
        assertEquals("foo.png", p.getChildren().get(0).getAttributes().get("src"));
    }

    @Test public void testParsesRoughAttributes() {
        TokenStream tokenStream = TokenStream.create("<html><head><title>First!</title></head><body><p class=\"foo > bar\">First post! <img src=\"foo.png\" /></p></body></html>");
        Parser parser = new Parser(tokenStream);
        Document doc = parser.parse();
        // need a better way to verify these:
        Element p = doc.getChildren().get(1).getChildren().get(0);
        assertEquals("p", p.getTagName());
        assertEquals("foo > bar", p.getAttributes().get("class"));
        assertEquals("foo.png", p.getChildren().get(0).getAttributes().get("src"));
    }

    @Test public void testParsesComments() {
        TokenStream ts = TokenStream.create("<html><head></head><body><!-- <table><tr><td></table> --><p>Hello</p></body></html>");
        Document doc = new Parser(ts).parse();
        Element body = doc.getChildren().get(1);
        Comment comment = (Comment) body.getChildNodes().get(0);
        assertEquals("<table><tr><td></table>", comment.getData());
        Element p = body.getChildren().get(0);
        TextNode text = (TextNode) p.getChildNodes().get(0);
        assertEquals("Hello", text.getWholeText());
    }
}
