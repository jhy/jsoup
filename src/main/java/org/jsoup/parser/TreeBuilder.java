package org.jsoup.parser;

import org.jsoup.UncheckedIOException;
import org.jsoup.helper.DataUtil;
import org.jsoup.helper.DataUtil.BomCharset;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jonathan Hedley
 */
abstract class TreeBuilder {
    protected Parser parser;
    CharacterReader reader;
    Tokeniser tokeniser;
    protected Document doc; // current doc we are building into
    protected ArrayList<Element> stack; // the stack of open elements
    protected String baseUri; // current base uri, for creating new elements
    protected Token currentToken; // currentToken is used only for error tracking.
    protected ParseSettings settings;

    private Token.StartTag start = new Token.StartTag(); // start tag to process
    private Token.EndTag end  = new Token.EndTag();
    abstract ParseSettings defaultSettings();

    protected void initialiseParse(Reader input, String baseUri, Parser parser) {
        Validate.notNull(input, "String input must not be null");
        Validate.notNull(baseUri, "BaseURI must not be null");

        doc = new Document(baseUri);
        doc.parser(parser);
        this.parser = parser;
        settings = parser.settings();
        
        this.handleSkipBomCharacter(input);
        reader = new CharacterReader(input);
        currentToken = null;
        tokeniser = new Tokeniser(reader, parser.getErrors());
        stack = new ArrayList<>(32);
        this.baseUri = baseUri;
    }

    Document parse(Reader input, String baseUri, Parser parser) {
        initialiseParse(input, baseUri, parser);
        runParser();
        return doc;
    }

    abstract List<Node> parseFragment(String inputFragment, Element context, String baseUri, Parser parser);

    protected void runParser() {
        while (true) {
            Token token = tokeniser.read();
            process(token);
            token.reset();

            if (token.type == Token.TokenType.EOF)
                break;
        }
    }

    protected abstract boolean process(Token token);

    protected boolean processStartTag(String name) {
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag().name(name));
        }
        return process(start.reset().name(name));
    }

    public boolean processStartTag(String name, Attributes attrs) {
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag().nameAttr(name, attrs));
        }
        start.reset();
        start.nameAttr(name, attrs);
        return process(start);
    }

    protected boolean processEndTag(String name) {
        if (currentToken == end) { // don't recycle an in-use token
            return process(new Token.EndTag().name(name));
        }
        return process(end.reset().name(name));
    }


    protected Element currentElement() {
        int size = stack.size();
        return size > 0 ? stack.get(size-1) : null;
    }
    
    /**
     * Checks if the first character is the BOM character in the input stream,
     *  if it is, skip the BOM character to avoid errors in parsing it.
     * @param input
     */
    private void handleSkipBomCharacter(Reader input) {
        try {
            // set up a char array buffer of size 10
            int bufferLength = 10;
            char[] charArray = new char[bufferLength];
            
            // mark the input stream to reset it later
            input.mark(bufferLength);
            // read the stream to the char array
            input.read(charArray, 0, bufferLength);
            // convert the char array to a byte buffer
            CharBuffer charBuffer = CharBuffer.wrap(charArray);
            ByteBuffer byteData = Charset.defaultCharset().encode(charBuffer);
            // detect BOM charset (reuse same util function)
            BomCharset bomCharset = DataUtil.detectCharsetFromBom(byteData);
            
            // reset the input stream
            input.reset();
            // if the first character is BOM character, skip it
            if (bomCharset != null && bomCharset.offset) { // creating the buffered reader ignores the input pos, so must skip here
                input.skip(1);
            }
        } catch (IOException e) {
            // wrap IOException with jsoup UncheckedIOException
            throw new UncheckedIOException(e);
        }
    }
}
