package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.List;

/**
 * Parses HTML into a {@link org.jsoup.nodes.Document}. Generally best to use one of the  more convenient parse methods
 * in {@link org.jsoup.Jsoup}.
 */
public class Parser {
    private static final int DEFAULT_MAX_ERRORS = 0; // by default, error tracking is disabled.
    
    private TreeBuilder treeBuilder;
    private int maxErrors = DEFAULT_MAX_ERRORS;
    private ParseErrorList errors;
    private ParseSettings settings;

    /**
     * Create a new Parser, using the specified TreeBuilder
     * @param treeBuilder TreeBuilder to use to parse input into Documents.
     */
    public Parser(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
        settings = treeBuilder.defaultSettings();
    }
    
    public Document parseInput(String html, String baseUri) {
        errors = isTrackErrors() ? ParseErrorList.tracking(maxErrors) : ParseErrorList.noTracking();
        return treeBuilder.parse(html, baseUri, errors, settings);
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
     * Update the TreeBuilder used when parsing content.
     * @param treeBuilder current TreeBuilder
     * @return this, for chaining
     */
    public Parser setTreeBuilder(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
        return this;
    }

    /**
     * Check if parse error tracking is enabled.
     * @return current track error state.
     */
    public boolean isTrackErrors() {
        return maxErrors > 0;
    }

    /**
     * Enable or disable parse error tracking for the next parse.
     * @param maxErrors the maximum number of errors to track. Set to 0 to disable.
     * @return this, for chaining
     */
    public Parser setTrackErrors(int maxErrors) {
        this.maxErrors = maxErrors;
        return this;
    }

    /**
     * Retrieve the parse errors, if any, from the last parse.
     * @return list of parse errors, up to the size of the maximum errors tracked.
     */
    public List<ParseError> getErrors() {
        return errors;
    }

    public Parser settings(ParseSettings settings) {
        this.settings = settings;
        return this;
    }

    public ParseSettings settings() {
        return settings;
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
        return treeBuilder.parse(html, baseUri, ParseErrorList.noTracking(), treeBuilder.defaultSettings());
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
        return treeBuilder.parseFragment(fragmentHtml, context, baseUri, ParseErrorList.noTracking(), treeBuilder.defaultSettings());
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
        return treeBuilder.parseFragment(fragmentXml, baseUri, ParseErrorList.noTracking(), treeBuilder.defaultSettings());
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
        Node[] nodes = nodeList.toArray(new Node[nodeList.size()]); // the node list gets modified when re-parented
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
        Tokeniser tokeniser = new Tokeniser(new CharacterReader(string), ParseErrorList.noTracking());
        return tokeniser.unescapeEntities(inAttribute);
    }

    /**
     * @param bodyHtml HTML to parse
     * @param baseUri baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     *
     * @return parsed Document
     * @deprecated Use {@link #parseBodyFragment} or {@link #parseFragment} instead.
     */
    public static Document parseBodyFragmentRelaxed(String bodyHtml, String baseUri) {
        return parse(bodyHtml, baseUri);
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
