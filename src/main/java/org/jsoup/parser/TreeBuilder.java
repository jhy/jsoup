package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.LineMap;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.NodeInternals;
import org.jsoup.select.NodeVisitor;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jsoup.parser.Parser.NamespaceHtml;

/**
 * @author Jonathan Hedley
 */
abstract class TreeBuilder {
    protected Parser parser;
    CharacterReader reader;
    Tokeniser tokeniser;
    Document doc; // current doc we are building into
    final ArrayList<Element> stack = new ArrayList<>(); // open elements only; the document is never on this stack
    String baseUri; // current base uri, for creating new elements
    Token currentToken; // currentToken is used for error and source position tracking. Null at start of fragment parse
    ParseSettings settings;
    TagSet tagSet; // the tags we're using in this parse
    @Nullable NodeVisitor nodeListener; // optional listener for node add / removes

    private Token.StartTag start; // start tag to process
    private final Token.EndTag end  = new Token.EndTag(this);
    abstract ParseSettings defaultSettings();

    boolean trackSourceRange; // optionally tracks source ranges of nodes and attributes
    @Nullable LineMap lineMap; // shared line map for retained source ranges
    private boolean parseComplete; // true only after EOF has closed the document

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
        reader.trackNewlines(parser.isTrackErrors() || trackSourceRange);
        lineMap = trackSourceRange ? reader.lineMap() : null;
        if (parser.isTrackErrors()) parser.getErrors().clear();
        tokeniser = new Tokeniser(this);
        stack.clear();
        stack.ensureCapacity(32);
        parseComplete = false;
        tagSet = parser.tagSet();
        start = new Token.StartTag(this);
        currentToken = start; // init current token to the virtual start token.
        this.baseUri = baseUri;
        onNodeInserted(doc);
    }

    /** Closes the current input and releases parse resources without changing whether EOF was reached. */
    void closeParse() {
        // tidy up - as the Parser and Treebuilder are retained in document for settings / fragments
        if (reader == null) return;
        if (lineMap != null) lineMap.complete();
        reader.close();
        reader = null;
        lineMap = null;
        tokeniser = null;
        stack.clear();
        stack.trimToSize();
    }

    Document parse(Reader input, String baseUri, Parser parser) {
        initialiseParse(input, baseUri, parser);
        runParser();
        return doc;
    }

    List<Node> parseFragment(Reader inputFragment, @Nullable Element context, String baseUri, Parser parser) {
        initialiseParse(inputFragment, baseUri, parser);
        initialiseParseFragment(context);
        runParser();
        return completeParseFragment();
    }

    void initialiseParseFragment(@Nullable Element context) {
        // in Html, sets up context; no-op in XML
    }

    abstract List<Node> completeParseFragment();

    /** Set the node listener, which will then get callbacks for node insert and removals. */
    void nodeListener(NodeVisitor nodeListener) {
        this.nodeListener = nodeListener;
    }

    /**
     Create a new copy of this TreeBuilder
     @return copy, ready for a new parse
     */
    abstract TreeBuilder newInstance();

    void runParser() {
        do {} while (stepParser()); // run until stepParser sees EOF
        closeParse();
    }

    boolean stepParser() {
        if (parseComplete) return false;

        // if we have reached the end already, step by popping off the stack, to hit nodeRemoved callbacks:
        if (currentToken.type == Token.TokenType.EOF) {
            if (stack.isEmpty()) {
                onNodeClosed(doc); // the root doc is not on the stack, so let this final step close it
                parseComplete = true;
                return true;
            }
            pop();
            return true;
        }
        final Token token = tokeniser.read();
        currentToken = token;
        process(token);
        token.reset();
        return true;
    }

    /** Return if we have reached EOF and completed the document tree. */
    boolean isComplete() {
        return parseComplete;
    }

    /** Check if the given Element is currently on the stack of open elements. */
    boolean isOpen(Element element) {
        return stack.contains(element);
    }

    /** Copies the currently open elements before a caller-initiated close clears the parser stack. */
    void copyOpenElementsTo(Collection<? super Element> elements) {
        elements.addAll(stack);
    }

    abstract boolean process(Token token);

    boolean processStartTag(String name) {
        // these are "virtual" start tags (auto-created by the treebuilder), so not tracking the start position
        final Token.StartTag start = this.start;
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag(this).name(name));
        }
        return process(start.reset().name(name));
    }

    boolean processStartTag(String name, Attributes attrs) {
        final Token.StartTag start = this.start;
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag(this).nameAttr(name, attrs));
        }
        start.reset();
        start.nameAttr(name, attrs);
        return process(start);
    }

    boolean processEndTag(String name) {
        if (currentToken == end) { // don't recycle an in-use token
            return process(new Token.EndTag(this).name(name));
        }
        return process(end.reset().name(name));
    }

    /**
     Removes the last Element from the stack, hits onNodeClosed, and then returns it.
     * @return
     */
    Element pop() {
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
     Ensures the stack respects {@link Parser#getMaxDepth()} by closing the deepest open elements until there is room for
     a new insertion.
     */
    final void enforceStackDepthLimit() {
        final int maxDepth = parser.getMaxDepth();
        if (maxDepth == Integer.MAX_VALUE) return;
        while (stack.size() >= maxDepth) {
            Element trimmed = pop();
            onStackPrunedForDepth(trimmed);
        }
    }

    /**
     Hook for the HTML Tree Builder that needs to clean up when an element is removed due to the depth limit
     */
    void onStackPrunedForDepth(Element element) {
        // default no-op
    }

    /**
     Default maximum depth for parsers using this tree builder.
     */
    int defaultMaxDepth() {
        return 512;
    }

    /**
     Gets the current open element for tree-construction decisions.
     The stack must not be empty; use {@link #hasCurrentElement()} when it may be.
     */
    Element currentElement() {
        return stack.get(stack.size() - 1);
    }

    /**
     Tests if there is a current open element.
     */
    boolean hasCurrentElement() {
        return !stack.isEmpty();
    }

    /**
     Gets the current open element, or the document when there is none.
     This fallback is for generic node attachment; state-specific placement and foster parenting use explicit targets.
     */
    Element currentElOrDoc() {
        return hasCurrentElement() ? currentElement() : doc;
    }

    /**
     Checks if the Current Element's normal name equals the supplied name, in the HTML namespace.
     @param normalName name to check
     @return true if there is a current element on the stack, and its name equals the supplied
     */
    boolean currentElementIs(String normalName) {
        return currentElementIs(normalName, NamespaceHtml);
    }

    /**
     Checks if the Current Element's normal name equals the supplied name, in the specified namespace.
     @param normalName name to check
     @param namespace the namespace
     @return true if there is a current element on the stack, and its name equals the supplied
     */
    boolean currentElementIs(String normalName, String namespace) {
        if (!hasCurrentElement())
            return false;
        Element current = currentElement();
        return current.normalName().equals(normalName)
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

    Tag tagFor(String tagName, String normalName, String namespace, ParseSettings settings) {
        return tagSet.valueOf(tagName, normalName, namespace, settings.preserveTagCase());
    }

    Tag tagFor(Token.Tag token) {
        return tagSet.valueOf(token.name(), token.normalName, defaultNamespace(), settings.preserveTagCase());
    }

    /**
     Gets the default namespace for this TreeBuilder
     * @return the default namespace
     */
    String defaultNamespace() {
        return NamespaceHtml;
    }

    TagSet defaultTagSet() {
        return TagSet.Html();
    }

    /**
     Called by implementing TreeBuilders when a node has been inserted. This implementation includes optionally tracking
     the source range of the node.  @param node the node that was just inserted
     */
    void onNodeInserted(Node node) {
        trackNodePosition(node, true);

        if (nodeListener != null)
            nodeListener.head(node, stack.size());
    }

    /**
     Called by implementing TreeBuilders when a node is explicitly closed. This implementation includes optionally
     tracking the closing source range of the node.  @param node the node being closed
     */
    void onNodeClosed(Node node) {
        trackNodePosition(node, false);

        if (nodeListener != null)
            nodeListener.tail(node, stack.size());
    }

    void trackNodePosition(Node node, boolean isStart) {
        if (!trackSourceRange) return;

        if (isStart && node.sourceRange().isTracked())
            return; // recreated nodes retain the position of their originating token
        if (!isStart && node instanceof Element && ((Element) node).endSourceRange().isTracked())
            return; // an element's first close is its source close

        final Token token = currentToken;
        int startPos = token.startPos();
        int endPos = token.endPos();

        // handle implicit element open / closes.
        if (node instanceof Element) {
            final Element el = (Element) node;
            if (token.isEOF()) {
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

        if (isStart)
            NodeInternals.sourceRange(node, lineMap(), startPos, endPos);
        else if (node instanceof Element)
            NodeInternals.endSourceRange((Element) node, lineMap(), startPos, endPos);
    }

    /**
     Internal method, used by parser tokens to attach source ranges to Nodes and Attributes.
     */
    LineMap lineMap() {
        assert lineMap != null;
        return lineMap;
    }
}
