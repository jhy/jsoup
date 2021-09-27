package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected Map<String, Tag> seenTags; // tags we've used in this parse; saves tag GC for custom tags.

    private Token.StartTag start = new Token.StartTag(); // start tag to process
    private Token.EndTag end  = new Token.EndTag();
    abstract ParseSettings defaultSettings();

    @ParametersAreNonnullByDefault
    protected void initialiseParse(Reader input, String baseUri, Parser parser) {
        Validate.notNull(input, "String input must not be null");
        Validate.notNull(baseUri, "BaseURI must not be null");
        Validate.notNull(parser);

        doc = new Document(baseUri);
        doc.parser(parser);
        this.parser = parser;
        settings = parser.settings();
        reader = new CharacterReader(input);
        reader.trackNewlines(parser.isTrackErrors()); // when tracking errors, enable newline tracking for better error reports
        currentToken = null;
        tokeniser = new Tokeniser(reader, parser.getErrors());
        stack = new ArrayList<>(32);
        seenTags = new HashMap<>();
        this.baseUri = baseUri;
    }

    @ParametersAreNonnullByDefault
    Document parse(Reader input, String baseUri, Parser parser) {
        initialiseParse(input, baseUri, parser);
        runParser();

        // tidy up - as the Parser and Treebuilder are retained in document for settings / fragments
        reader.close();
        reader = null;
        tokeniser = null;
        stack = null;
        seenTags = null;

        return doc;
    }

    /**
     Create a new copy of this TreeBuilder
     @return copy, ready for a new parse
     */
    abstract TreeBuilder newInstance();

    abstract List<Node> parseFragment(String inputFragment, Element context, String baseUri, Parser parser);

    protected void runParser() {
        final Tokeniser tokeniser = this.tokeniser;
        final Token.TokenType eof = Token.TokenType.EOF;

        while (true) {
            Token token = tokeniser.read();
            process(token);
            token.reset();

            if (token.type == eof)
                break;
        }
    }

    protected abstract boolean process(Token token);

    protected boolean processStartTag(String name) {
        final Token.StartTag start = this.start;
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag().name(name));
        }
        return process(start.reset().name(name));
    }

    public boolean processStartTag(String name, Attributes attrs) {
        final Token.StartTag start = this.start;
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


    /**
     Get the current element (last on the stack). If all items have been removed, returns the document instead
     (which might not actually be on the stack; use stack.size() == 0 to test if required.
     @return the last element on the stack, if any; or the root document
     */
    protected Element currentElement() {
        int size = stack.size();
        return size > 0 ? stack.get(size-1) : doc;
    }

    /**
     Checks if the Current Element's normal name equals the supplied name.
     @param normalName name to check
     @return true if there is a current element on the stack, and its name equals the supplied
     */
    protected boolean currentElementIs(String normalName) {
        if (stack.size() == 0)
            return false;
        Element current = currentElement();
        return current != null && current.normalName().equals(normalName);
    }

    /**
     * If the parser is tracking errors, add an error at the current position.
     * @param msg error message
     */
    protected void error(String msg) {
        error(msg, (Object[]) null);
    }

    /**
     * If the parser is tracking errors, add an error at the current position.
     * @param msg error message template
     * @param args template arguments
     */
    protected void error(String msg, Object... args) {
        ParseErrorList errors = parser.getErrors();
        if (errors.canAddError())
            errors.add(new ParseError(reader, msg, args));
    }

    /**
     (An internal method, visible for Element. For HTML parse, signals that script and style text should be treated as
     Data Nodes).
     */
    protected boolean isContentForTagData(String normalName) {
        return false;
    }

    protected Tag tagFor(String tagName, ParseSettings settings) {
        Tag tag = seenTags.get(tagName); // note that we don't normalize the cache key. But tag via valueOf may be normalized.
        if (tag == null) {
            tag = Tag.valueOf(tagName, settings);
            seenTags.put(tagName, tag);
        }
        return tag;
    }
}
