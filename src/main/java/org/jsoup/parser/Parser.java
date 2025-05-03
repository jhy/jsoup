package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 Parses HTML or XML into a {@link org.jsoup.nodes.Document}. Generally, it is simpler to use one of the parse methods in
 {@link org.jsoup.Jsoup}.
 <p>Note that a given Parser instance object is threadsafe, but not concurrent. (Concurrent parse calls will
 synchronize.) To reuse a Parser configuration in a multithreaded environment, use {@link #newInstance()} to make
 copies.</p>
 */
public class Parser implements Cloneable {
    public static final String NamespaceHtml = "http://www.w3.org/1999/xhtml";
    public static final String NamespaceXml = "http://www.w3.org/XML/1998/namespace";
    public static final String NamespaceMathml = "http://www.w3.org/1998/Math/MathML";
    public static final String NamespaceSvg = "http://www.w3.org/2000/svg";

    private final TreeBuilder treeBuilder;
    private ParseErrorList errors;
    private ParseSettings settings;
    private boolean trackPosition = false;
    private @Nullable TagSet tagSet;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Create a new Parser, using the specified TreeBuilder
     * @param treeBuilder TreeBuilder to use to parse input into Documents.
     */
    public Parser(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
        settings = treeBuilder.defaultSettings();
        errors = ParseErrorList.noTracking();
    }

    /**
     Creates a new Parser as a deep copy of this; including initializing a new TreeBuilder. Allows independent (multi-threaded) use.
     @return a copied parser
     */
    public Parser newInstance() {
        return new Parser(this);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod") // because we use the copy constructor instead
    @Override
    public Parser clone() {
        return new Parser(this);
    }

    private Parser(Parser copy) {
        treeBuilder = copy.treeBuilder.newInstance(); // because extended
        errors = new ParseErrorList(copy.errors); // only copies size, not contents
        settings = new ParseSettings(copy.settings);
        trackPosition = copy.trackPosition;
    }
    
    public Document parseInput(String html, String baseUri) {
        return parseInput(new StringReader(html), baseUri);
    }

    public Document parseInput(Reader inputHtml, String baseUri) {
        try {
            lock.lock(); // using a lock vs synchronized to support loom threads
            return treeBuilder.parse(inputHtml, baseUri, this);
        } finally {
            lock.unlock();
        }
    }

    public List<Node> parseFragmentInput(String fragment, @Nullable Element context, String baseUri) {
        return parseFragmentInput(new StringReader(fragment), context, baseUri);
    }

    public List<Node> parseFragmentInput(Reader fragment, @Nullable Element context, String baseUri) {
        try {
            lock.lock();
            return treeBuilder.parseFragment(fragment, context, baseUri, this);
        } finally {
            lock.unlock();
        }
    }

    // gets & sets
    /**
     * Get the TreeBuilder currently in use.
     * @return current TreeBuilder.
     */
    public TreeBuilder getTreeBuilder() {
        return treeBuilder;
    }

    /**
     * Check if parse error tracking is enabled.
     * @return current track error state.
     */
    public boolean isTrackErrors() {
        return errors.getMaxSize() > 0;
    }

    /**
     * Enable or disable parse error tracking for the next parse.
     * @param maxErrors the maximum number of errors to track. Set to 0 to disable.
     * @return this, for chaining
     */
    public Parser setTrackErrors(int maxErrors) {
        errors = maxErrors > 0 ? ParseErrorList.tracking(maxErrors) : ParseErrorList.noTracking();
        return this;
    }

    /**
     * Retrieve the parse errors, if any, from the last parse.
     * @return list of parse errors, up to the size of the maximum errors tracked.
     * @see #setTrackErrors(int)
     */
    public ParseErrorList getErrors() {
        return errors;
    }

    /**
     Test if position tracking is enabled. If it is, Nodes will have a Position to track where in the original input
     source they were created from. By default, tracking is not enabled.
     * @return current track position setting
     */
    public boolean isTrackPosition() {
        return trackPosition;
    }

    /**
     Enable or disable source position tracking. If enabled, Nodes will have a Position to track where in the original
     input source they were created from.
     @param trackPosition position tracking setting; {@code true} to enable
     @return this Parser, for chaining
     */
    public Parser setTrackPosition(boolean trackPosition) {
        this.trackPosition = trackPosition;
        return this;
    }

    /**
     Update the ParseSettings of this Parser, to control the case sensitivity of tags and attributes.
     * @param settings the new settings
     * @return this Parser
     */
    public Parser settings(ParseSettings settings) {
        this.settings = settings;
        return this;
    }

    /**
     Gets the current ParseSettings for this Parser
     * @return current ParseSettings
     */
    public ParseSettings settings() {
        return settings;
    }

    /**
     Set a custom TagSet to use for this Parser. This allows you to define your own tags, and control how they are
     parsed. For example, you can set a tag to preserve whitespace, or to be treated as a block tag.
     <p>You can start with the {@link TagSet#Html()} defaults and customize, or a new empty TagSet.</p>

     @param tagSet the TagSet to use. This gets copied, so that changes that the parse makes (tags found in the document will be added) do not clobber the original TagSet.
     @return this Parser
     @since 1.20.1
     */
    public Parser tagSet(TagSet tagSet) {
        Validate.notNull(tagSet);
        this.tagSet = new TagSet(tagSet); // copy it as we are going to mutate it
        return this;
    }

    /**
     Get the current TagSet for this Parser, which will be either this parser's default, or one that you have set.
     @return the current TagSet. After the parse, this will contain any new tags that were found in the document.
     @since 1.20.1
     */
    public TagSet tagSet() {
        if (tagSet == null)
            tagSet = treeBuilder.defaultTagSet();
        return tagSet;
    }

    public String defaultNamespace() {
        return getTreeBuilder().defaultNamespace();
    }

    // static parse functions below
    /**
     * Parse HTML into a Document.
     *
     * @param html HTML to parse
     * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     *
     * @return parsed Document
     */
    public static Document parse(String html, String baseUri) {
        TreeBuilder treeBuilder = new HtmlTreeBuilder();
        return treeBuilder.parse(new StringReader(html), baseUri, new Parser(treeBuilder));
    }

    /**
     * Parse a fragment of HTML into a list of nodes. The context element, if supplied, supplies parsing context.
     *
     * @param fragmentHtml the fragment of HTML to parse
     * @param context (optional) the element that this HTML fragment is being parsed for (i.e. for inner HTML). This
     * provides stack context (for implicit element creation).
     * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     *
     * @return list of nodes parsed from the input HTML. Note that the context element, if supplied, is not modified.
     */
    public static List<Node> parseFragment(String fragmentHtml, Element context, String baseUri) {
        HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
        return treeBuilder.parseFragment(new StringReader(fragmentHtml), context, baseUri, new Parser(treeBuilder));
    }

    /**
     * Parse a fragment of HTML into a list of nodes. The context element, if supplied, supplies parsing context.
     *
     * @param fragmentHtml the fragment of HTML to parse
     * @param context (optional) the element that this HTML fragment is being parsed for (i.e. for inner HTML). This
     * provides stack context (for implicit element creation).
     * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @param errorList list to add errors to
     *
     * @return list of nodes parsed from the input HTML. Note that the context element, if supplied, is not modified.
     */
    public static List<Node> parseFragment(String fragmentHtml, Element context, String baseUri, ParseErrorList errorList) {
        HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
        Parser parser = new Parser(treeBuilder);
        parser.errors = errorList;
        return treeBuilder.parseFragment(new StringReader(fragmentHtml), context, baseUri, parser);
    }

    /**
     * Parse a fragment of XML into a list of nodes.
     *
     * @param fragmentXml the fragment of XML to parse
     * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @return list of nodes parsed from the input XML.
     */
    public static List<Node> parseXmlFragment(String fragmentXml, String baseUri) {
        XmlTreeBuilder treeBuilder = new XmlTreeBuilder();
        return treeBuilder.parseFragment(new StringReader(fragmentXml), null, baseUri, new Parser(treeBuilder));
    }

    /**
     * Parse a fragment of HTML into the {@code body} of a Document.
     *
     * @param bodyHtml fragment of HTML
     * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     *
     * @return Document, with empty head, and HTML parsed into body
     */
    public static Document parseBodyFragment(String bodyHtml, String baseUri) {
        Document doc = Document.createShell(baseUri);
        Element body = doc.body();
        List<Node> nodeList = parseFragment(bodyHtml, body, baseUri);
        Node[] nodes = nodeList.toArray(new Node[0]); // the node list gets modified when re-parented
        for (int i = nodes.length - 1; i > 0; i--) {
            nodes[i].remove();
        }
        for (Node node : nodes) {
            body.appendChild(node);
        }
        return doc;
    }

    /**
     * Utility method to unescape HTML entities from a string
     * @param string HTML escaped string
     * @param inAttribute if the string is to be escaped in strict mode (as attributes are)
     * @return an unescaped string
     */
    public static String unescapeEntities(String string, boolean inAttribute) {
        Parser parser = Parser.htmlParser();
        parser.treeBuilder.initialiseParse(new StringReader(string), "", parser);
        Tokeniser tokeniser = new Tokeniser(parser.treeBuilder);
        return tokeniser.unescapeEntities(inAttribute);
    }

    // builders

    /**
     * Create a new HTML parser. This parser treats input as HTML5, and enforces the creation of a normalised document,
     * based on a knowledge of the semantics of the incoming tags.
     * @return a new HTML parser.
     */
    public static Parser htmlParser() {
        return new Parser(new HtmlTreeBuilder());
    }

    /**
     * Create a new XML parser. This parser assumes no knowledge of the incoming tags and does not treat it as HTML,
     * rather creates a simple tree directly from the input.
     * @return a new simple XML parser.
     */
    public static Parser xmlParser() {
        return new Parser(new XmlTreeBuilder());
    }
}
