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

    public static String clean(String html, Whitelist whitelist) {
        Document dirty = Jsoup.parse("<body>" + html); // TODO: modify parser to El = Parser.parseBodyFragment
        Cleaner cleaner = new Cleaner(whitelist);
        Document clean = cleaner.clean(dirty);
        return clean.getBody().html();
    }
}
