package org.jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import java.io.File;
import java.io.IOException;
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
     Parse HTML into a Document. As no base URI is specified, absolute URL detection relies on the HTML including a
     {@code <base href>} tag.

     @param html HTML to parse
     @return sane HTML

     @see #parse(String, String)
     */
    public static Document parse(String html) {
        return Parser.parse(html, "");
    }

    /**
     Fetch a URL, and parse it as HTML.

     @param url           URL to fetch (with a GET). The protocol must be {@code http} or {@code https}.
     @param timeoutMillis Connection and read timeout, in milliseconds. If exceeded, IOException is thrown.
     @return The parsed HTML.

     @throws IOException If the final server response != 200 OK (redirects are followed), or if there's an error reading
     the response stream.
     */
    public static Document parse(URL url, int timeoutMillis) throws IOException {
        String html = DataUtil.load(url, timeoutMillis);
        return parse(html, url.toExternalForm());
    }

    /**
     Parse the contents of a file as HTML.

     @param in          file to load HTML from
     @param charsetName character set of file contents. If you don't know the charset, generally the best guess is {@code UTF-8}.
     @param baseUri     The URL where the HTML was retrieved from, to generate absolute URLs relative to.
     @return sane HTML

     @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    public static Document parse(File in, String charsetName, String baseUri) throws IOException {
        String html = DataUtil.load(in, charsetName);
        return parse(html, baseUri);
    }

    /**
     Parse the contents of a file as HTML. The location of the file is used as the base URI to qualify relative URLs.

     @param in          file to load HTML from
     @param charsetName character set of file contents. If you don't know the charset, generally the best guess is {@code UTF-8}.
     @return sane HTML

     @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     @see #parse(File, String, String)
     */
    public static Document parse(File in, String charsetName) throws IOException {
        String html = DataUtil.load(in, charsetName);
        return parse(html, in.getAbsolutePath());
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
     Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through a white-list of permitted
     tags and attributes.

     @param bodyHtml  input untrusted HMTL
     @param baseUri   URL to resolve relative URLs against
     @param whitelist white-list of permitted HTML elements
     @return safe HTML

     @see Cleaner#clean(Document)
     */
    public static String clean(String bodyHtml, String baseUri, Whitelist whitelist) {
        Document dirty = parseBodyFragment(bodyHtml, baseUri);
        Cleaner cleaner = new Cleaner(whitelist);
        Document clean = cleaner.clean(dirty);
        return clean.body().html();
    }

    /**
     Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through a white-list of permitted
     tags and attributes.

     @param bodyHtml  input untrusted HTML
     @param whitelist white-list of permitted HTML elements
     @return safe HTML

     @see Cleaner#clean(Document)
     */
    public static String clean(String bodyHtml, Whitelist whitelist) {
        return clean(bodyHtml, "", whitelist);
    }

    /**
     Test if the input HTML has only tags and attributes allowed by the Whitelist. Useful for form validation. The input HTML should
     still be run through the cleaner to set up enforced attributes, and to tidy the output.
     @param bodyHtml HTML to test
     @param whitelist whitelist to test against
     @return true if no tags or attributes were removed; false otherwise
     @see #clean(String, org.jsoup.safety.Whitelist) 
     */
    public static boolean isValid(String bodyHtml, Whitelist whitelist) {
        Document dirty = parseBodyFragment(bodyHtml, "");
        Cleaner cleaner = new Cleaner(whitelist);
        return cleaner.isValid(dirty);
    }
    
}
