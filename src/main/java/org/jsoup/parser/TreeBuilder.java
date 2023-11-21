package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.SharedConstants;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Range;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jsoup.parser.Parser.NamespaceHtml;

/**
 * @author Jonathan Hedley
 */
abstract class TreeBuilder {
    protected Parser parser;
    CharacterReader reader;
    Tokeniser tokeniser;
    Document doc; // current doc we are building into
    ArrayList<Element> stack; // the stack of open elements
    String baseUri; // current base uri, for creating new elements
    Token currentToken; // currentToken is used only for error tracking.
    ParseSettings settings;
    Map<String, Tag> seenTags; // tags we've used in this parse; saves tag GC for custom tags.

    private Token.StartTag start; // start tag to process
    private final Token.EndTag end  = new Token.EndTag();
    abstract ParseSettings defaultSettings();

    private boolean trackSourceRange;  // optionally tracks the source range of nodes

    void initialiseParse(Reader input, String baseUri, Parser parser) {
        Validate.notNullParam(input, "input");
        Validate.notNullParam(baseUri, "baseUri");
        Validate.notNull(parser);

        doc = new Document(parser.defaultNamespace(), baseUri);
        doc.parser(parser);
        this.parser = parser;
        settings = parser.settings();
        reader = new CharacterReader(input);
        trackSourceRange = parser.isTrackPosition();
        reader.trackNewlines(parser.isTrackErrors() || trackSourceRange); // when tracking errors or source ranges, enable newline tracking for better legibility
        currentToken = null;
        tokeniser = new Tokeniser(reader, parser.getErrors(), trackSourceRange);
        stack = new ArrayList<>(32);
        seenTags = new HashMap<>();
        start = new Token.StartTag(trackSourceRange, reader);
        this.baseUri = baseUri;
    }

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

    void runParser() {
        final Tokeniser tokeniser = this.tokeniser;
        final Token.TokenType eof = Token.TokenType.EOF;

        while (true) {
            Token token = tokeniser.read();
            currentToken = token;
            process(token);
            if (token.type == eof)
                break;
            token.reset();
        }

        // once we hit the end, pop remaining items off the stack
        while (!stack.isEmpty()) pop();
    }

    abstract boolean process(Token token);

    boolean processStartTag(String name) {
        // these are "virtual" start tags (auto-created by the treebuilder), so not tracking the start position
        final Token.StartTag start = this.start;
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag(trackSourceRange, reader).name(name));
        }
        return process(start.reset().name(name));
    }

    boolean processStartTag(String name, Attributes attrs) {
        final Token.StartTag start = this.start;
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag(trackSourceRange, reader).nameAttr(name, attrs));
        }
        start.reset();
        start.nameAttr(name, attrs);
        return process(start);
    }

    boolean processEndTag(String name) {
        if (currentToken == end) { // don't recycle an in-use token
            return process(new Token.EndTag().name(name));
        }
        return process(end.reset().name(name));
    }

    /**
     Removes the last Element from the stack, hits onNodeClosed, and then returns it.
     * @return
     */
    final Element pop() {
        int size = stack.size();
        Element removed = stack.remove(size - 1);
        onNodeClosed(removed);
        return removed;
    }

    /**
     Adds the specified Element to the end of the stack, and hits onNodeInserted.
     * @param element
     */
    final void push(Element element) {
        stack.add(element);
        onNodeInserted(element);
    }

    /**
     Get the current element (last on the stack). If all items have been removed, returns the document instead
     (which might not actually be on the stack; use stack.size() == 0 to test if required.
     @return the last element on the stack, if any; or the root document
     */
    Element currentElement() {
        int size = stack.size();
        return size > 0 ? stack.get(size-1) : doc;
    }

    /**
     Checks if the Current Element's normal name equals the supplied name, in the HTML namespace.
     @param normalName name to check
     @return true if there is a current element on the stack, and its name equals the supplied
     */
    boolean currentElementIs(String normalName) {
        if (stack.size() == 0)
            return false;
        Element current = currentElement();
        return current != null && current.normalName().equals(normalName)
            && current.tag().namespace().equals(NamespaceHtml);
    }

    /**
     Checks if the Current Element's normal name equals the supplied name, in the specified namespace.
     @param normalName name to check
     @param namespace the namespace
     @return true if there is a current element on the stack, and its name equals the supplied
     */
    boolean currentElementIs(String normalName, String namespace) {
        if (stack.size() == 0)
            return false;
        Element current = currentElement();
        return current != null && current.normalName().equals(normalName)
            && current.tag().namespace().equals(namespace);
    }

    /**
     * If the parser is tracking errors, add an error at the current position.
     * @param msg error message
     */
    void error(String msg) {
        error(msg, (Object[]) null);
    }

    /**
     * If the parser is tracking errors, add an error at the current position.
     * @param msg error message template
     * @param args template arguments
     */
    void error(String msg, Object... args) {
        ParseErrorList errors = parser.getErrors();
        if (errors.canAddError())
            errors.add(new ParseError(reader, msg, args));
    }

    /**
     (An internal method, visible for Element. For HTML parse, signals that script and style text should be treated as
     Data Nodes).
     */
    boolean isContentForTagData(String normalName) {
        return false;
    }

    Tag tagFor(String tagName, String namespace, ParseSettings settings) {
        Tag cached = seenTags.get(tagName); // note that we don't normalize the cache key. But tag via valueOf may be normalized.
        if (cached == null || !cached.namespace().equals(namespace)) {
            // only return from cache if the namespace is the same. not running nested cache to save double hit on the common flow
            Tag tag = Tag.valueOf(tagName, namespace, settings);
            seenTags.put(tagName, tag);
            return tag;
        }
        return cached;
    }

    Tag tagFor(String tagName, ParseSettings settings) {
        return tagFor(tagName, defaultNamespace(), settings);
    }

    /**
     Gets the default namespace for this TreeBuilder
     * @return the default namespace
     */
    String defaultNamespace() {
        return NamespaceHtml;
    }

    /**
     Called by implementing TreeBuilders when a node has been inserted. This implementation includes optionally tracking
     the source range of the node.  @param node the node that was just inserted
     */
    void onNodeInserted(Node node) {
        trackNodePosition(node, true);
    }

    /**
     Called by implementing TreeBuilders when a node is explicitly closed. This implementation includes optionally
     tracking the closing source range of the node.  @param node the node being closed
     */
    void onNodeClosed(Node node) {
        trackNodePosition(node, false);
    }

    private void trackNodePosition(Node node, boolean isStart) {
        if (!trackSourceRange) return;

        final Token token = currentToken;
        int startPos = token.startPos();
        int endPos = token.endPos();

        // handle implicit element open / closes.
        if (node instanceof Element) {
            final Element el = (Element) node;
            if (token.isEOF()) {
                if (el.endSourceRange().isTracked())
                    return; // /body and /html are left on stack until EOF, don't reset them
                startPos = endPos = reader.pos();
            } else if (isStart) { // opening tag
                if  (!token.isStartTag() || !el.normalName().equals(token.asStartTag().normalName)) {
                    endPos = startPos;
                }
            } else { // closing tag
                if (!el.tag().isEmpty() && !el.tag().isSelfClosing()) {
                    if (!token.isEndTag() || !el.normalName().equals(token.asEndTag().normalName)) {
                        endPos = startPos;
                    }
                }
            }
        }

        Range.Position startPosition = new Range.Position
            (startPos, reader.lineNumber(startPos), reader.columnNumber(startPos));
        Range.Position endPosition = new Range.Position
            (endPos, reader.lineNumber(endPos), reader.columnNumber(endPos));
        Range range = new Range(startPosition, endPosition);
        range.track(node, isStart);
    }
}
