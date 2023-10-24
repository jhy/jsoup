package org.jsoup;

import org.jsoup.helper.DataUtil;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import javax.annotation.Nullable;
import javax.annotation.WillClose;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 The core public access point to the jsoup functionality.

 @author Jonathan Hedley */

public class Jsoup {
    private Jsoup() {}

    /**
     Parse HTML into a Document. The parser will make a sensible, balanced document tree out of any HTML.

     @param html    HTML to parse
     @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     before the HTML declares a {@code <base href>} tag.
     @return sane HTML
     */
    public static Document parse(String html, String baseUri) {
        return Parser.parse(html, baseUri);
    }

    /**
     Parse HTML into a Document, using the provided Parser. You can provide an alternate parser, such as a simple XML
     (non-HTML) parser.

     @param html    HTML to parse
     @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     before the HTML declares a {@code <base href>} tag.
     @param parser alternate {@link Parser#xmlParser() parser} to use.
     @return sane HTML
     */
    public static Document parse(String html, String baseUri, Parser parser) {
        return parser.parseInput(html, baseUri);
    }

    /**
     Parse HTML into a Document, using the provided Parser. You can provide an alternate parser, such as a simple XML
     (non-HTML) parser.  As no base URI is specified, absolute URL resolution, if required, relies on the HTML including
     a {@code <base href>} tag.

     @param html    HTML to parse
     before the HTML declares a {@code <base href>} tag.
     @param parser alternate {@link Parser#xmlParser() parser} to use.
     @return sane HTML
     */
    public static Document parse(String html, Parser parser) {
        return parser.parseInput(html, "");
    }

    /**
     Parse HTML into a Document. As no base URI is specified, absolute URL resolution, if required, relies on the HTML
     including a {@code <base href>} tag.

     @param html HTML to parse
     @return sane HTML

     @see #parse(String, String)
     */
    public static Document parse(String html) {
        return Parser.parse(html, "");
    }

    /**
     * Creates a new {@link Connection} (session), with the defined request URL. Use to fetch and parse a HTML page.
     * <p>
     * Use examples:
     * <ul>
     *  <li><code>Document doc = Jsoup.connect("http://example.com").userAgent("Mozilla").data("name", "jsoup").get();</code></li>
     *  <li><code>Document doc = Jsoup.connect("http://example.com").cookie("auth", "token").post();</code></li>
     * </ul>
     * @param url URL to connect to. The protocol must be {@code http} or {@code https}.
     * @return the connection. You can add data, cookies, and headers; set the user-agent, referrer, method; and then execute.
     * @see #newSession()
     * @see Connection#newRequest()
     */
    public static Connection connect(String url) {
        return HttpConnection.connect(url);
    }

    /**
     Creates a new {@link Connection} to use as a session. Connection settings (user-agent, timeouts, URL, etc), and
     cookies will be maintained for the session. Use examples:
<pre><code>
Connection session = Jsoup.newSession()
     .timeout(20 * 1000)
     .userAgent("FooBar 2000");

Document doc1 = session.newRequest()
     .url("https://jsoup.org/").data("ref", "example")
     .get();
Document doc2 = session.newRequest()
     .url("https://en.wikipedia.org/wiki/Main_Page")
     .get();
Connection con3 = session.newRequest();
</code></pre>

     <p>For multi-threaded requests, it is safe to use this session between threads, but take care to call {@link
    Connection#newRequest()} per request and not share that instance between threads when executing or parsing.</p>

     @return a connection
     @since 1.14.1
     */
    public static Connection newSession() {
        return new HttpConnection();
    }

    /**
     Parse the contents of a file as HTML.

     @param file          file to load HTML from. Supports gzipped files (ending in .z or .gz).
     @param charsetName (optional) character set of file contents. Set to {@code null} to determine from {@code http-equiv} meta tag, if
     present, or fall back to {@code UTF-8} (which is often safe to do).
     @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     @return sane HTML

     @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    public static Document parse(File file, @Nullable String charsetName, String baseUri) throws IOException {
        return DataUtil.load(file, charsetName, baseUri);
    }

    /**
     Parse the contents of a file as HTML. The location of the file is used as the base URI to qualify relative URLs.

     @param file        file to load HTML from. Supports gzipped files (ending in .z or .gz).
     @param charsetName (optional) character set of file contents. Set to {@code null} to determine from {@code http-equiv} meta tag, if
     present, or fall back to {@code UTF-8} (which is often safe to do).
     @return sane HTML

     @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     @see #parse(File, String, String) parse(file, charset, baseUri)
     */
    public static Document parse(File file, @Nullable String charsetName) throws IOException {
        return DataUtil.load(file, charsetName, file.getAbsolutePath());
    }

    /**
     Parse the contents of a file as HTML. The location of the file is used as the base URI to qualify relative URLs.
     The charset used to read the file will be determined by the byte-order-mark (BOM), or a {@code <meta charset>} tag,
     or if neither is present, will be {@code UTF-8}.

     <p>This is the equivalent of calling {@link #parse(File, String) parse(file, null)}</p>

     @param file the file to load HTML from. Supports gzipped files (ending in .z or .gz).
     @return sane HTML
     @throws IOException if the file could not be found or read.
     @see #parse(File, String, String) parse(file, charset, baseUri)
     @since 1.15.1
     */
    public static Document parse(File file) throws IOException {
        return DataUtil.load(file, null, file.getAbsolutePath());
    }

    /**
     Parse the contents of a file as HTML.

     @param file          file to load HTML from. Supports gzipped files (ending in .z or .gz).
     @param charsetName (optional) character set of file contents. Set to {@code null} to determine from {@code http-equiv} meta tag, if
     present, or fall back to {@code UTF-8} (which is often safe to do).
     @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     @param parser alternate {@link Parser#xmlParser() parser} to use.
     @return sane HTML

     @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     @since 1.14.2
     */
    public static Document parse(File file, @Nullable String charsetName, String baseUri, Parser parser) throws IOException {
        return DataUtil.load(file, charsetName, baseUri, parser);
    }

     /**
     Read an input stream, and parse it to a Document.

     @param in          input stream to read. The stream will be closed after reading.
     @param charsetName (optional) character set of file contents. Set to {@code null} to determine from {@code http-equiv} meta tag, if
     present, or fall back to {@code UTF-8} (which is often safe to do).
     @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     @return sane HTML

     @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    public static Document parse(@WillClose InputStream in, @Nullable String charsetName, String baseUri) throws IOException {
        return DataUtil.load(in, charsetName, baseUri);
    }

    /**
     Read an input stream, and parse it to a Document. You can provide an alternate parser, such as a simple XML
     (non-HTML) parser.

     @param in          input stream to read. Make sure to close it after parsing.
     @param charsetName (optional) character set of file contents. Set to {@code null} to determine from {@code http-equiv} meta tag, if
     present, or fall back to {@code UTF-8} (which is often safe to do).
     @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     @param parser alternate {@link Parser#xmlParser() parser} to use.
     @return sane HTML

     @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    public static Document parse(InputStream in, @Nullable String charsetName, String baseUri, Parser parser) throws IOException {
        return DataUtil.load(in, charsetName, baseUri, parser);
    }

    /**
     Parse a fragment of HTML, with the assumption that it forms the {@code body} of the HTML.

     @param bodyHtml body HTML fragment
     @param baseUri  URL to resolve relative URLs against.
     @return sane HTML document

     @see Document#body()
     */
    public static Document parseBodyFragment(String bodyHtml, String baseUri) {
        return Parser.parseBodyFragment(bodyHtml, baseUri);
    }

    /**
     Parse a fragment of HTML, with the assumption that it forms the {@code body} of the HTML.

     @param bodyHtml body HTML fragment
     @return sane HTML document

     @see Document#body()
     */
    public static Document parseBodyFragment(String bodyHtml) {
        return Parser.parseBodyFragment(bodyHtml, "");
    }

    /**
     Fetch a URL, and parse it as HTML. Provided for compatibility; in most cases use {@link #connect(String)} instead.
     <p>
     The encoding character set is determined by the content-type header or http-equiv meta tag, or falls back to {@code UTF-8}.

     @param url           URL to fetch (with a GET). The protocol must be {@code http} or {@code https}.
     @param timeoutMillis Connection and read timeout, in milliseconds. If exceeded, IOException is thrown.
     @return The parsed HTML.

     @throws java.net.MalformedURLException if the request URL is not a HTTP or HTTPS URL, or is otherwise malformed
     @throws HttpStatusException if the response is not OK and HTTP response errors are not ignored
     @throws UnsupportedMimeTypeException if the response mime type is not supported and those errors are not ignored
     @throws java.net.SocketTimeoutException if the connection times out
     @throws IOException if a connection or read error occurs

     @see #connect(String)
     */
    public static Document parse(URL url, int timeoutMillis) throws IOException {
        Connection con = HttpConnection.connect(url);
        con.timeout(timeoutMillis);
        return con.get();
    }

    /**
     Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through an allow-list of safe
     tags and attributes.

     @param bodyHtml  input untrusted HTML (body fragment)
     @param baseUri   URL to resolve relative URLs against
     @param safelist  list of permitted HTML elements
     @return safe HTML (body fragment)

     @see Cleaner#clean(Document)
     */
    public static String clean(String bodyHtml, String baseUri, Safelist safelist) {
        Document dirty = parseBodyFragment(bodyHtml, baseUri);
        Cleaner cleaner = new Cleaner(safelist);
        Document clean = cleaner.clean(dirty);
        return clean.body().html();
    }

    /**
     Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through a safe-list of permitted
     tags and attributes.

     <p>Note that as this method does not take a base href URL to resolve attributes with relative URLs against, those
     URLs will be removed, unless the input HTML contains a {@code <base href> tag}. If you wish to preserve those, use
     the {@link Jsoup#clean(String html, String baseHref, Safelist)} method instead, and enable
     {@link Safelist#preserveRelativeLinks(boolean)}.</p>

     <p>Note that the output of this method is still <b>HTML</b> even when using the TextNode only
     {@link Safelist#none()}, and so any HTML entities in the output will be appropriately escaped.
     If you want plain text, not HTML, you should use a text method such as {@link Element#text()} instead, after
     cleaning the document.</p>
     <p>Example:</p>
     <pre>{@code
     String sourceBodyHtml = "<p>5 is &lt; 6.</p>";
     String html = Jsoup.clean(sourceBodyHtml, Safelist.none());

     Cleaner cleaner = new Cleaner(Safelist.none());
     String text = cleaner.clean(Jsoup.parse(sourceBodyHtml)).text();

     // html is: 5 is &lt; 6.
     // text is: 5 is < 6.
     }</pre>

     @param bodyHtml input untrusted HTML (body fragment)
     @param safelist list of permitted HTML elements
     @return safe HTML (body fragment)
     @see Cleaner#clean(Document)
     */
    public static String clean(String bodyHtml, Safelist safelist) {
        return clean(bodyHtml, "", safelist);
    }

    /**
     * Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through a safe-list of
     * permitted tags and attributes.
     * <p>The HTML is treated as a body fragment; it's expected the cleaned HTML will be used within the body of an
     * existing document. If you want to clean full documents, use {@link Cleaner#clean(Document)} instead, and add
     * structural tags (<code>html, head, body</code> etc) to the safelist.
     *
     * @param bodyHtml input untrusted HTML (body fragment)
     * @param baseUri URL to resolve relative URLs against
     * @param safelist list of permitted HTML elements
     * @param outputSettings document output settings; use to control pretty-printing and entity escape modes
     * @return safe HTML (body fragment)
     * @see Cleaner#clean(Document)
     */
    public static String clean(String bodyHtml, String baseUri, Safelist safelist, Document.OutputSettings outputSettings) {
        Document dirty = parseBodyFragment(bodyHtml, baseUri);
        Cleaner cleaner = new Cleaner(safelist);
        Document clean = cleaner.clean(dirty);
        clean.outputSettings(outputSettings);
        return clean.body().html();
    }

    /**
     Test if the input body HTML has only tags and attributes allowed by the Safelist. Useful for form validation.
     <p>
     This method is intended to be used in a user interface as a validator for user input. Note that regardless of the
     output of this method, the input document <b>must always</b> be normalized using a method such as
     {@link #clean(String, String, Safelist)}, and the result of that method used to store or serialize the document
     before later reuse such as presentation to end users. This ensures that enforced attributes are set correctly, and
     that any differences between how a given browser and how jsoup parses the input HTML are normalized.
     </p>
     <p>Example:</p>
     <pre>{@code
     Safelist safelist = Safelist.relaxed();
     boolean isValid = Jsoup.isValid(sourceBodyHtml, safelist);
     String normalizedHtml = Jsoup.clean(sourceBodyHtml, "https://example.com/", safelist);
     }</pre>
     <p>Assumes the HTML is a body fragment (i.e. will be used in an existing HTML document body.)
     @param bodyHtml HTML to test
     @param safelist safelist to test against
     @return true if no tags or attributes were removed; false otherwise
     @see #clean(String, Safelist)
     */
    public static boolean isValid(String bodyHtml, Safelist safelist) {
        return new Cleaner(safelist).isValidBodyHtml(bodyHtml);
    }
}
