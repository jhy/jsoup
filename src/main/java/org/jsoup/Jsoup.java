package org.jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

/**
 * Jsoup main entry point.
 *
 * @author Jonathan Hedley
 */
public class Jsoup {
    public static Document parse(String html) {
        return Parser.parse(html);
    }
}
