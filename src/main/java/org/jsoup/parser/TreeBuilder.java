package org.jsoup.parser;

import org.jsoup.helper.DescendableLinkedList;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jonathan Hedley
 */
abstract class TreeBuilder {
    CharacterReader reader;
    Tokeniser tokeniser;
    protected Document doc; // current doc we are building into
    protected DescendableLinkedList<Element> stack; // the stack of open elements
    protected String baseUri; // current base uri, for creating new elements
    protected Token currentToken; // currentToken is used only for error tracking.
    protected boolean trackErrors = false;
    protected List<ParseError> errors;

    protected void initialiseParse(String input, String baseUri, boolean trackErrors) {
        doc = new Document(baseUri);
        reader = new CharacterReader(input);
        tokeniser = new Tokeniser(reader);
        stack = new DescendableLinkedList<Element>();
        this.baseUri = baseUri;
        errors = new ArrayList<ParseError>();
        this.trackErrors = trackErrors;
    }

    Document parse(String input, String baseUri) {
        return parse(input, baseUri, false);
    }

    Document parse(String input, String baseUri, boolean trackErrors) {
        initialiseParse(input, baseUri, trackErrors);
        runParser();
        return doc;
    }
    
    List<ParseError> getErrors() {
        return errors;
    }

    protected void runParser() {
        while (true) {
            Token token = tokeniser.read();
            process(token);

            if (token.type == Token.TokenType.EOF)
                break;
        }
    }

    protected abstract boolean process(Token token);

    protected Element currentElement() {
        return stack.getLast();
    }
}
