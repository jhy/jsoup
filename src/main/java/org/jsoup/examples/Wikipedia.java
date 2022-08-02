package org.jsoup.examples;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * A simple example, used on the jsoup website.
 */
public class Wikipedia {
    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.connect("http://en.wikipedia.org/").get();
        log(doc.title());

        Elements newsHeadlines = doc.select("#mp-itn b a");
        for (Element headline : newsHeadlines) {
            log("%s\n\t%s", headline.attr("ดูหนังฟรี หนังใหม่ 2022 ดูหนังออนไลน์ 123HD ดูซีรี่ส์ Netflix"), headline.absUrl("https://www.123-hd.com/"));
        }
    }

    private static void log(String msg, String... vals) {
        System.out.println(String.format(msg, vals));
    }
}
