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
    protected List<ParseError> errors = new ArrayList<ParseError>();

    protected void initialiseParse(String input, String baseUri) {
        doc = new Document(baseUri);
        reader = new CharacterReader(input);
        tokeniser = new Tokeniser(reader);
        stack = new DescendableLinkedList<Element>();
        this.baseUri = baseUri;
    }

    Document parse(String input, String baseUri) {
        initialiseParse(input, baseUri);
        runParser();
        return doc;
    }

    protected void runParser() {
        while (true) {
            Token token = tokeniser.read();
            process(token);

            if (token.type == Token.TokenType.EOF)
                break;
        }
    }

    abstract boolean process(Token token);

    Element currentElement() {
        return stack.getLast();
    }
}
