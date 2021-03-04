package org.jsoup.examples;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A simple example, used on the jsoup website.
 */
public class Wikipedia {
    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.connect("http://en.wikipedia.org/").get();
        log(doc.title());

        Elements newsHeadlines = doc.select("#mp-itn b a");
        for (Element headline : newsHeadlines) {
            log("%s\n\t%s", headline.attr("title"), headline.absUrl("href"));
        }
        Cleaner cln = new Cleaner(new Safelist());
        cln.trackDiscAttribs();
        cln.trackDiscElems();
        cln.clean(doc);

        ArrayList<Node> elems = cln.getDiscElems();
        ArrayList<Node> attribs = cln.getDiscAttribs();
        log("Hej \n");

    }

    private static void log(String msg, String... vals) {
        System.out.println(String.format(msg, vals));
    }
}
