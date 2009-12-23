package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
}
