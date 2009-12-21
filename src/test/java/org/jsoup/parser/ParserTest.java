package org.jsoup.parser;

import org.jsoup.nodes.Document;
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
    }
}
