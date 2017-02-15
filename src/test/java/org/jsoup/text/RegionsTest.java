package org.jsoup.text;

import org.junit.Test;
import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.parser.Parser;


/**
 *  Regions tests.
 */
public class RegionsTest {
    @Test
    public void removesMinimally() {
        Document html = Jsoup.parse("<p>word blah <b>word</b> blah word");
        Element p = html.select("p").first();
        p.find("word").remove();
        assertEquals("<p> blah  blah </p>", p.outerHtml());

        html = Jsoup.parse("<div><p>word <b>word</b><p><b>blah word");
        Element div = html.select("div").first();
        div.find("word blah").remove();
        assertEquals("<div><p>word </p><p><b> word</b></p></div>",
                     TextUtil.stripNewlines(div.outerHtml()));

        html = Jsoup.parse("<p>wo<span>rdwordwo</span>rd</p>");
        p = html.select("p").first();
        Regions words = p.find("rdwo");
        assertEquals(2, words.size());
        assertEquals(0, words.get(0).getStart().getTextNode().siblingIndex());
        assertEquals(1, words.get(1).getStart().getTextNode().siblingIndex());
        words.remove();
        assertEquals("word", p.html());
    }

    @Test
    public void wraps() {
        Document html = Jsoup.parse("<p>Partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        p.find("olden").first().splitByElements().wrap("<i>");
        assertEquals("<p>Partly em<b>b<i>old</i></b><i>en</i>ed words.</p>",
                     p.outerHtml());
    }
}
