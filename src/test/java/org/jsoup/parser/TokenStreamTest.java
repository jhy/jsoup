package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;


/**
 TokenStream test cases.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TokenStreamTest {
    @Test
    public void createTokenStreamFromString() {
        List<Token> tokens = TokenStream.create("<html><body title='x > y'><p>Hello world!</p></body></html>").asList();
        assertEquals("Correct num tokens", 7, tokens.size());
    }

    // TODO: test data, positions, line breaks
}
