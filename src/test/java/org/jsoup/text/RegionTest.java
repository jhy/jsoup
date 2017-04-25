package org.jsoup.text;

import org.junit.Test;
import static org.junit.Assert.*;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.parser.Parser;

/**
 *  Region tests.
 */
public class RegionTest {
    @Test
    public void findsSimpleRegion() {
        Document html = Jsoup.parse("<p>Three words here");
        Region words = html.find("words").first();
        assertNotNull(words);
        assertEquals("words", words.getStart().getTextNode().getWholeText());
    }

    @Test
    public void findsParagraphSpanningRegion() {
        Document html = Jsoup.parse("<p>Four words<p>about love.");
        Region words = html.find("words about").first();
        assertNotNull(words);
        assertEquals("words", words.getStart().getTextNode().text());
        assertEquals("about", words.getEnd().getTextNode().text());
        assertEquals("words about", words.getText());
    }

    @Test
    public void findsBrSpanningRegion() {
        Document html = Jsoup.parse("<p>Four words<br>about love.");
        Region words = html.find("words about").first();
        assertNotNull(words);
        assertEquals("words", words.getStart().getTextNode().text());
        assertEquals("about", words.getEnd().getTextNode().text());
        assertEquals("words about", words.getText());
        // and for bonus harshness, try it on a Document with fewer
        // spaces than what Jsoup.parse() produces.
        for(Element p : html.select("p"))
            for(TextNode t : p.textNodes())
                t.text(t.text());
        for(Element p : html.select("br"))
            for(TextNode t : p.textNodes())
                t.text(t.text());
        assertEquals(1, html.find("words about").size());
    }

    @Test
    public void findsPartlyBoldText() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Region words = html.find("emboldened").first();
        assertNotNull(words);
        assertEquals("em", words.getStart().getTextNode().text());
        assertEquals("ened", words.getEnd().getTextNode().text());
        assertEquals("emboldened", words.getText());

        assertEquals(0, html.find("em bold").size());
    }

    @Test
    public void findsProSHyCessShyedWords() {
        Document html = Jsoup.parse("<p>Cleverly pro&shy;cess&shy;ed text here");
        Region processed = html.find("processed").first();
        assertEquals("pro\u00ADcess\u00ADed", processed.getText());
    }


    @Test
    public void splitsAtElementBoundary() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Region words = html.find("emboldened").first();
        assertNotNull(words);
        Regions parts = words.splitByElements();
        assertEquals(3, parts.size());
        assertEquals("em", parts.first().getText());
        assertEquals("bold", parts.get(1).getText());
        assertEquals("ened", parts.get(2).getText());
    }

    @Test
    public void handlesEmptyRegion() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Region emboldened = html.find("emboldened").first();
        assertNotNull(emboldened);
        Region empty = new Region(emboldened.getStart(), emboldened.getStart());
        assertEquals(0, empty.splitByElements().size());
        assertEquals(0, empty.splitByBlockElements().size());
        empty.trim();
        assertEquals(emboldened.getStart(), empty.getStart());
        assertEquals(emboldened.getStart(), empty.getEnd());
        assertEquals("", empty.getText());

        Region dot = html.find(".").first();
        // next assertion breaks if the region doesn't end at the end
        // of the document
        assertEquals(dot.getEnd(), dot.getEnd().rightBound());
        empty = new Region(dot.getEnd(), dot.getEnd());
        assertEquals(0, empty.splitByElements().size());
        assertEquals(0, empty.splitByBlockElements().size());
        empty.trim();
        assertEquals(empty.getEnd(), empty.getStart());
        assertEquals("", empty.getText());
    }

    @Test
    public void entireElementRegions() {
        Document html = Jsoup.parse("<p><i>Partly em<b>bold</b></i>ened blah.");
        Element i = html.select("i").first();
        assertEquals("Partly embold", new Region(i).getText());
        assertEquals("bold", new Region(html.select("b").first()).getText());
        i.after("<div><img></div>");
        Region div = new Region(html.select("div").first());
        assertEquals("", div.getText());
        assertNotEquals(0, html.select("div").first().textNodes().size());
    }

    @Test
    public void splitsTextNodes() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();

        Region emboldened = html.find("emboldened").first();
        emboldened.splitTextNodes();
        assertEquals("emboldened", emboldened.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        Region four = html.find("Four").first();
        four.splitTextNodes();
        assertEquals("Four", four.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        Region partly = html.find("partly").first();
        assertEquals("partly", partly.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        Region old = html.find("old").first();
        old.splitTextNodes();
        assertEquals("old", old.getText());
        assertEquals("<p>Four partly em<b>bold</b>ened words.</p>",
                     p.outerHtml());
    }


    @Test
    public void findsExternalParent() {
        Document html = Jsoup.parse("<p>Three <i>for<b>matte</b>d</i> words.");
        Element p = html.select("p").first();
        Element i = html.select("i").first();
        assertEquals(p, p.find("Three").first().parentElement());
        assertEquals(i, p.find("matte").first().parentElement());
        assertEquals(i, p.find("ormatted").first().parentElement());
        assertEquals(p, p.find("formatted").first().parentElement());
    }


    @Test
    public void splitsSpans() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        p.find("olden").first().ensureRemovability();
        assertEquals("<p>Four partly em<b>b</b><b>old</b>ened words.</p>",
                     html.select("p").first().outerHtml());

        p.find("old").first().ensureRemovability();
        assertEquals("<p>Four partly em<b>b</b><b>old</b>ened words.</p>",
                     p.outerHtml());

        Region partly = p.find("partly").first();
        partly.ensureRemovability();
        assertEquals("partly", partly.getStart().getTextNode().text());
        assertEquals("<p>Four partly em<b>b</b><b>old</b>ened words.</p>",
                     p.outerHtml());
    }

    @Test
    public void removesMinimally() {
        Document html = Jsoup.parse("<p>Four partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        Region partly = p.find("partly").first();
        assertEquals(p, partly.parentElement());
        assertEquals(1, partly.parents().size());
        assertEquals("partly", partly.parents().get(0).toString());
        partly.remove();
        assertEquals("<p>Four  em<b>bold</b>ened words.</p>",
                     p.outerHtml());

        p.find("bold").remove();
        assertEquals("<p>Four  emened words.</p>",
                     p.outerHtml());
    }

    @Test
    public void wraps() {
        Document html = Jsoup.parse("<p>Partly em<b>bold</b>ened words.");
        Element p = html.select("p").first();
        p.find("Partly").first().wrap("<i>");
        assertEquals("<p><i>Partly</i> em<b>bold</b>ened words.</p>",
                     p.outerHtml());
        p.find("olden").first().wrap("<i>");
        assertEquals("<p><i>Partly</i> em<b>b</b><i><b>old</b>en</i>ed words.</p>",
                     p.outerHtml());
    }

    @Test
    public void trims() {
        Document html = Jsoup.parse("<p>Some partly <b>bold</b> words about life.");
        Element p = html.select("p").first();
        Region all = p.find("partly bold words").first();
        Region bold = p.find("bold").first();
        Region prelude = new Region(all.getStart(), bold.getStart());
        Region postlude = new Region(bold.getEnd(), all.getEnd());
        assertEquals("partly bold words", all.getText());
        assertEquals("partly", prelude.getText());
        assertEquals("words", postlude.getText());
        prelude.trim();
        assertEquals("partly", prelude.getText());
        postlude.trim();
        assertEquals("words", postlude.getText());
        assertEquals("partly bold words", all.getText());

        html = Jsoup.parse("<p>one <p>two");
        all = html.find("one two").first();
        assertNotNull(all);
        Region one = html.find("one").first();
        assertNotNull(one);
        Region rest = new Region(one.getEnd(), all.getEnd());
        assertEquals("two", rest.getText());
        rest.getStart().getTextNode().text("\n\n");
        assertEquals("two", rest.getText());
        rest.trim();
        assertEquals("two", rest.getText());
        rest.getEnd().getTextNode().text("2\n\n");
        rest.trim();
        assertEquals("2", rest.getText());
    }
}
