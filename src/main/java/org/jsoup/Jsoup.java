package org.jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 * Jsoup main entry point.
 *
 * @author Jonathan Hedley
 */
public class Jsoup {
    public static Document parse(String html, String baseUri) {
        return Parser.parse(html, baseUri);
    }

    public static Document parse(String html) {
        return Parser.parse(html, "");
    }
    
    public static Document parseBodyFragment(String bodyHtml, String baseUri) {
        return Parser.parseBodyFragment(bodyHtml, baseUri);
    }
    
    public static Document parseBodyFragment(String bodyHtml) {
        return Parser.parseBodyFragment(bodyHtml, "");
    }

    public static String clean(String bodyHtml, String baseUri, Whitelist whitelist) {
        Document dirty = parseBodyFragment(bodyHtml, baseUri);
        Cleaner cleaner = new Cleaner(whitelist);
        Document clean = cleaner.clean(dirty);
        return clean.body().html();
    }
    
    public static String clean(String bodyHtml, Whitelist whitelist) {
        return clean(bodyHtml, "", whitelist);
    }
}
