package org.jsoup.nodes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.DataUtil;
import org.jsoup.helper.Validate;
import org.jsoup.internal.StringUtil;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;

import static org.jsoup.parser.Parser.NamespaceHtml;

/**
 A HTML Document.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Document extends Element {
    private @Nullable Connection connection; // the connection this doc was fetched from, if any
    private OutputSettings outputSettings = new OutputSettings();
    private Parser parser; // the parser used to parse this document
    private QuirksMode quirksMode = QuirksMode.noQuirks;
    private final String location;
    private boolean updateMetaCharset = false;

    /**
     Create a new, empty Document, in the specified namespace.
     @param namespace the namespace of this Document's root node.
     @param baseUri base URI of document
     @see org.jsoup.Jsoup#parse
     @see #createShell
     */
    public Document(String namespace, String baseUri) {
        super(Tag.valueOf("#root", namespace, ParseSettings.htmlDefault), baseUri);
        this.location = baseUri;
        this.parser = Parser.htmlParser(); // default, but overridable
    }

    /**
     Create a new, empty Document, in the HTML namespace.
     @param baseUri base URI of document
     @see org.jsoup.Jsoup#parse
     @see #Document(String namespace, String baseUri)
     */
    public Document(String baseUri) {
        this(NamespaceHtml, baseUri);
    }

    /**
     Create a valid, empty shell of a document, suitable for adding more elements to.
     @param baseUri baseUri of document
     @return document with html, head, and body elements.
     */
    public static Document createShell(String baseUri) {
        Validate.notNull(baseUri);

        Document doc = new Document(baseUri);
        doc.parser = doc.parser();
        Element html = doc.appendElement("html");
        html.appendElement("head");
        html.appendElement("body");

        return doc;
    }

    /**
     * Get the URL this Document was parsed from. If the starting URL is a redirect,
     * this will return the final URL from which the document was served from.
     * <p>Will return an empty string if the location is unknown (e.g. if parsed from a String).
     * @return location
     */
    public String location() {
        return location;
    }

    /**
     Returns the Connection (Request/Response) object that was used to fetch this document, if any; otherwise, a new
     default Connection object. This can be used to continue a session, preserving settings and cookies, etc.
     @return the Connection (session) associated with this Document, or an empty one otherwise.
     @see Connection#newRequest()
     */
    public Connection connection() {
        if (connection == null)
            return Jsoup.newSession();
        else
            return connection;
    }

    /**
     * Returns this Document's doctype.
     * @return document type, or null if not set
     */
    public @Nullable DocumentType documentType() {
        for (Node node : childNodes) {
            if (node instanceof DocumentType)
                return (DocumentType) node;
            else if (!(node instanceof LeafNode)) // scans forward across comments, text, processing instructions etc
                break;
        }
        return null;
        // todo - add a set document type?
    }

    /**
     Find the root HTML element, or create it if it doesn't exist.
     @return the root HTML element.
     */
    private Element htmlEl() {
        Element el = firstElementChild();
        while (el != null) {
            if (el.nameIs("html"))
                return el;
            el = el.nextElementSibling();
        }
        return appendElement("html");
    }

    /**
     Get this document's {@code head} element.
     <p>
     As a side-effect, if this Document does not already have a HTML structure, it will be created. If you do not want
     that, use {@code #selectFirst("head")} instead.

     @return {@code head} element.
     */
    public Element head() {
        final Element html = htmlEl();
        Element el = html.firstElementChild();
        while (el != null) {
            if (el.nameIs("head"))
                return el;
            el = el.nextElementSibling();
        }
        return html.prependElement("head");
    }

    /**
     Get this document's {@code <body>} or {@code <frameset>} element.
     <p>
     As a <b>side-effect</b>, if this Document does not already have a HTML structure, it will be created with a {@code
    <body>} element. If you do not want that, use {@code #selectFirst("body")} instead.

     @return {@code body} element for documents with a {@code <body>}, a new {@code <body>} element if the document
     had no contents, or the outermost {@code <frameset> element} for frameset documents.
     */
    public Element body() {
        final Element html = htmlEl();
        Element el = html.firstElementChild();
        while (el != null) {
            if (el.nameIs("body") || el.nameIs("frameset"))
                return el;
            el = el.nextElementSibling();
        }
        return html.appendElement("body");
    }

    /**
     Get each of the {@code <form>} elements contained in this document.
     @return a List of FormElement objects, which will be empty if there are none.
     @see Elements#forms()
     @see FormElement#elements()
     @since 1.15.4
     */
    public List<FormElement> forms() {
        return select("form").forms();
    }

    /**
     Selects the first {@link FormElement} in this document that matches the query. If none match, throws an
     {@link IllegalArgumentException}.
     @param cssQuery a {@link Selector} CSS query
     @return the first matching {@code <form>} element
     @throws IllegalArgumentException if no match is found
     @since 1.15.4
     */
    public FormElement expectForm(String cssQuery) {
        Elements els = select(cssQuery);
        for (Element el : els) {
            if (el instanceof FormElement) return (FormElement) el;
        }
        Validate.fail("No form elements matched the query '%s' in the document.", cssQuery);
        return null; // (not really)
    }

    /**
     Get the string contents of the document's {@code title} element.
     @return Trimmed title, or empty string if none set.
     */
    public String title() {
        // title is a preserve whitespace tag (for document output), but normalised here
        Element titleEl = head().selectFirst(titleEval);
        return titleEl != null ? StringUtil.normaliseWhitespace(titleEl.text()).trim() : "";
    }
    private static final Evaluator titleEval = new Evaluator.Tag("title");

    /**
     Set the document's {@code title} element. Updates the existing element, or adds {@code title} to {@code head} if
     not present
     @param title string to set as title
     */
    public void title(String title) {
        Validate.notNull(title);
        Element titleEl = head().selectFirst(titleEval);
        if (titleEl == null) // add to head
            titleEl = head().appendElement("title");
        titleEl.text(title);
    }

    /**
     Create a new Element, with this document's base uri. Does not make the new element a child of this document.
     @param tagName element tag name (e.g. {@code a})
     @return new element
     */
    public Element createElement(String tagName) {
        return new Element(Tag.valueOf(tagName, parser.defaultNamespace(), ParseSettings.preserveCase), this.baseUri());
    }

    @Override
    public String outerHtml() {
        return super.html(); // no outer wrapper tag
    }

    /**
     Set the text of the {@code body} of this document. Any existing nodes within the body will be cleared.
     @param text unencoded text
     @return this document
     */
    @Override
    public Element text(String text) {
        body().text(text); // overridden to not nuke doc structure
        return this;
    }

    @Override
    public String nodeName() {
        return "#document";
    }
    
    /**
     * Sets the charset used in this document. This method is equivalent
     * to {@link OutputSettings#charset(java.nio.charset.Charset)
     * OutputSettings.charset(Charset)} but in addition it updates the
     * charset / encoding element within the document.
     * 
     * <p>This enables
     * {@link #updateMetaCharsetElement(boolean) meta charset update}.</p>
     * 
     * <p>If there's no element with charset / encoding information yet it will
     * be created. Obsolete charset / encoding definitions are removed!</p>
     * 
     * <p><b>Elements used:</b></p>
     * 
     * <ul>
     * <li><b>Html:</b> <i>&lt;meta charset="CHARSET"&gt;</i></li>
     * <li><b>Xml:</b> <i>&lt;?xml version="1.0" encoding="CHARSET"&gt;</i></li>
     * </ul>
     * 
     * @param charset Charset
     * 
     * @see #updateMetaCharsetElement(boolean) 
     * @see OutputSettings#charset(java.nio.charset.Charset) 
     */
    public void charset(Charset charset) {
        updateMetaCharsetElement(true);
        outputSettings.charset(charset);
        ensureMetaCharsetElement();
    }
    
    /**
     * Returns the charset used in this document. This method is equivalent
     * to {@link OutputSettings#charset()}.
     * 
     * @return Current Charset
     * 
     * @see OutputSettings#charset() 
     */
    public Charset charset() {
        return outputSettings.charset();
    }
    
    /**
     * Sets whether the element with charset information in this document is
     * updated on changes through {@link #charset(java.nio.charset.Charset)
     * Document.charset(Charset)} or not.
     * 
     * <p>If set to <tt>false</tt> <i>(default)</i> there are no elements
     * modified.</p>
     * 
     * @param update If <tt>true</tt> the element updated on charset
     * changes, <tt>false</tt> if not
     * 
     * @see #charset(java.nio.charset.Charset) 
     */
    public void updateMetaCharsetElement(boolean update) {
        this.updateMetaCharset = update;
    }
    
    /**
     * Returns whether the element with charset information in this document is
     * updated on changes through {@link #charset(java.nio.charset.Charset)
     * Document.charset(Charset)} or not.
     * 
     * @return Returns <tt>true</tt> if the element is updated on charset
     * changes, <tt>false</tt> if not
     */
    public boolean updateMetaCharsetElement() {
        return updateMetaCharset;
    }

    @Override
    public Document clone() {
        Document clone = (Document) super.clone();
        clone.outputSettings = this.outputSettings.clone();
        return clone;
    }

    @Override
    public Document shallowClone() {
        Document clone = new Document(this.tag().namespace(), baseUri());
        if (attributes != null)
            clone.attributes = attributes.clone();
        clone.outputSettings = this.outputSettings.clone();
        return clone;
    }
    
    /**
     * Ensures a meta charset (html) or xml declaration (xml) with the current
     * encoding used. This only applies with
     * {@link #updateMetaCharsetElement(boolean) updateMetaCharset} set to
     * <tt>true</tt>, otherwise this method does nothing.
     * 
     * <ul>
     * <li>An existing element gets updated with the current charset</li>
     * <li>If there's no element yet it will be inserted</li>
     * <li>Obsolete elements are removed</li>
     * </ul>
     * 
     * <p><b>Elements used:</b></p>
     * 
     * <ul>
     * <li><b>Html:</b> <i>&lt;meta charset="CHARSET"&gt;</i></li>
     * <li><b>Xml:</b> <i>&lt;?xml version="1.0" encoding="CHARSET"&gt;</i></li>
     * </ul>
     */
    private void ensureMetaCharsetElement() {
        if (updateMetaCharset) {
            OutputSettings.Syntax syntax = outputSettings().syntax();

            if (syntax == OutputSettings.Syntax.html) {
                Element metaCharset = selectFirst("meta[charset]");
                if (metaCharset != null) {
                    metaCharset.attr("charset", charset().displayName());
                } else {
                    head().appendElement("meta").attr("charset", charset().displayName());
                }
                select("meta[name=charset]").remove(); // Remove obsolete elements
            } else if (syntax == OutputSettings.Syntax.xml) {
                Node node = ensureChildNodes().get(0);
                if (node instanceof XmlDeclaration) {
                    XmlDeclaration decl = (XmlDeclaration) node;
                    if (decl.name().equals("xml")) {
                        decl.attr("encoding", charset().displayName());
                        if (decl.hasAttr("version"))
                            decl.attr("version", "1.0");
                    } else {
                        decl = new XmlDeclaration("xml", false);
                        decl.attr("version", "1.0");
                        decl.attr("encoding", charset().displayName());
                        prependChild(decl);
                    }
                } else {
                    XmlDeclaration decl = new XmlDeclaration("xml", false);
                    decl.attr("version", "1.0");
                    decl.attr("encoding", charset().displayName());
                    prependChild(decl);
                }
            }
        }
    }
    

    /**
     * A Document's output settings control the form of the text() and html() methods.
     */
    public static class OutputSettings implements Cloneable {
        /**
         * The output serialization syntax.
         */
        public enum Syntax {html, xml}

        private Entities.EscapeMode escapeMode = Entities.EscapeMode.base;
        private Charset charset;
        Entities.CoreCharset coreCharset; // fast encoders for ascii and utf8
        private final ThreadLocal<CharsetEncoder> encoderThreadLocal = new ThreadLocal<>(); // initialized by start of OuterHtmlVisitor

        private boolean prettyPrint = true;
        private boolean outline = false;
        private int indentAmount = 1;
        private int maxPaddingWidth = 30;
        private Syntax syntax = Syntax.html;

        public OutputSettings() {
            charset(DataUtil.UTF_8);
        }
        
        /**
         * Get the document's current HTML escape mode: <code>base</code>, which provides a limited set of named HTML
         * entities and escapes other characters as numbered entities for maximum compatibility; or <code>extended</code>,
         * which uses the complete set of HTML named entities.
         * <p>
         * The default escape mode is <code>base</code>.
         * @return the document's current escape mode
         */
        public Entities.EscapeMode escapeMode() {
            return escapeMode;
        }

        /**
         * Set the document's escape mode, which determines how characters are escaped when the output character set
         * does not support a given character:- using either a named or a numbered escape.
         * @param escapeMode the new escape mode to use
         * @return the document's output settings, for chaining
         */
        public OutputSettings escapeMode(Entities.EscapeMode escapeMode) {
            this.escapeMode = escapeMode;
            return this;
        }

        /**
         * Get the document's current output charset, which is used to control which characters are escaped when
         * generating HTML (via the <code>html()</code> methods), and which are kept intact.
         * <p>
         * Where possible (when parsing from a URL or File), the document's output charset is automatically set to the
         * input charset. Otherwise, it defaults to UTF-8.
         * @return the document's current charset.
         */
        public Charset charset() {
            return charset;
        }

        /**
         * Update the document's output charset.
         * @param charset the new charset to use.
         * @return the document's output settings, for chaining
         */
        public OutputSettings charset(Charset charset) {
            this.charset = charset;
            coreCharset = Entities.CoreCharset.byName(charset.name());
            return this;
        }

        /**
         * Update the document's output charset.
         * @param charset the new charset (by name) to use.
         * @return the document's output settings, for chaining
         */
        public OutputSettings charset(String charset) {
            charset(Charset.forName(charset));
            return this;
        }

        CharsetEncoder prepareEncoder() {
            // created at start of OuterHtmlVisitor so each pass has own encoder, so OutputSettings can be shared among threads
            CharsetEncoder encoder = charset.newEncoder();
            encoderThreadLocal.set(encoder);
            return encoder;
        }

        CharsetEncoder encoder() {
            CharsetEncoder encoder = encoderThreadLocal.get();
            return encoder != null ? encoder : prepareEncoder();
        }

        /**
         * Get the document's current output syntax.
         * @return current syntax
         */
        public Syntax syntax() {
            return syntax;
        }

        /**
         * Set the document's output syntax. Either {@code html}, with empty tags and boolean attributes (etc), or
         * {@code xml}, with self-closing tags.
         * <p>When set to {@link Document.OutputSettings.Syntax#xml xml}, the {@link #escapeMode() escapeMode} is
         * automatically set to {@link Entities.EscapeMode#xhtml}, but may be subsequently changed if desired.</p>
         * @param syntax serialization syntax
         * @return the document's output settings, for chaining
         */
        public OutputSettings syntax(Syntax syntax) {
            this.syntax = syntax;
            if (syntax == Syntax.xml)
                this.escapeMode(Entities.EscapeMode.xhtml);
            return this;
        }

        /**
         * Get if pretty printing is enabled. Default is true. If disabled, the HTML output methods will not re-format
         * the output, and the output will generally look like the input.
         * @return if pretty printing is enabled.
         */
        public boolean prettyPrint() {
            return prettyPrint;
        }

        /**
         * Enable or disable pretty printing.
         * @param pretty new pretty print setting
         * @return this, for chaining
         */
        public OutputSettings prettyPrint(boolean pretty) {
            prettyPrint = pretty;
            return this;
        }
        
        /**
         * Get if outline mode is enabled. Default is false. If enabled, the HTML output methods will consider
         * all tags as block.
         * @return if outline mode is enabled.
         */
        public boolean outline() {
            return outline;
        }
        
        /**
         * Enable or disable HTML outline mode.
         * @param outlineMode new outline setting
         * @return this, for chaining
         */
        public OutputSettings outline(boolean outlineMode) {
            outline = outlineMode;
            return this;
        }

        /**
         * Get the current tag indent amount, used when pretty printing.
         * @return the current indent amount
         */
        public int indentAmount() {
            return indentAmount;
        }

        /**
         * Set the indent amount for pretty printing
         * @param indentAmount number of spaces to use for indenting each level. Must be {@literal >=} 0.
         * @return this, for chaining
         */
        public OutputSettings indentAmount(int indentAmount) {
            Validate.isTrue(indentAmount >= 0);
            this.indentAmount = indentAmount;
            return this;
        }

        /**
         * Get the current max padding amount, used when pretty printing
         * so very deeply nested nodes don't get insane padding amounts.
         * @return the current indent amount
         */
        public int maxPaddingWidth() {
            return maxPaddingWidth;
        }

        /**
         * Set the max padding amount for pretty printing so very deeply nested nodes don't get insane padding amounts.
         * @param maxPaddingWidth number of spaces to use for indenting each level of nested nodes. Must be {@literal >=} -1.
         *        Default is 30 and -1 means unlimited.
         * @return this, for chaining
         */
        public OutputSettings maxPaddingWidth(int maxPaddingWidth) {
            Validate.isTrue(maxPaddingWidth >= -1);
            this.maxPaddingWidth = maxPaddingWidth;
            return this;
        }

        @Override
        public OutputSettings clone() {
            OutputSettings clone;
            try {
                clone = (OutputSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            clone.charset(charset.name()); // new charset, coreCharset, and charset encoder
            clone.escapeMode = Entities.EscapeMode.valueOf(escapeMode.name());
            // indentAmount, maxPaddingWidth, and prettyPrint are primitives so object.clone() will handle
            return clone;
        }
    }

    /**
     * Get the document's current output settings.
     * @return the document's current output settings.
     */
    public OutputSettings outputSettings() {
        return outputSettings;
    }

    /**
     * Set the document's output settings.
     * @param outputSettings new output settings.
     * @return this document, for chaining.
     */
    public Document outputSettings(OutputSettings outputSettings) {
        Validate.notNull(outputSettings);
        this.outputSettings = outputSettings;
        return this;
    }

    public enum QuirksMode {
        noQuirks, quirks, limitedQuirks
    }

    public QuirksMode quirksMode() {
        return quirksMode;
    }

    public Document quirksMode(QuirksMode quirksMode) {
        this.quirksMode = quirksMode;
        return this;
    }

    /**
     * Get the parser that was used to parse this document.
     * @return the parser
     */
    public Parser parser() {
        return parser;
    }

    /**
     * Set the parser used to create this document. This parser is then used when further parsing within this document
     * is required.
     * @param parser the configured parser to use when further parsing is required for this document.
     * @return this document, for chaining.
     */
    public Document parser(Parser parser) {
        this.parser = parser;
        return this;
    }

    /**
     Set the Connection used to fetch this document. This Connection is used as a session object when further requests are
     made (e.g. when a form is submitted).

     @param connection to set
     @return this document, for chaining
     @see Connection#newRequest()
     @since 1.14.1
     */
    public Document connection(Connection connection) {
        Validate.notNull(connection);
        this.connection = connection;
        return this;
    }
}
