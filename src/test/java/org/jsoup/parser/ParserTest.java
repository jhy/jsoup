package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 Tests for the Parser

 @author Jonathan Hedley, jonathan@hedley.net */
public class ParserTest {

    @Test public void parsesSimpleDocument() {
        String html = "<html><head><title>First!</title></head><body><p>First post! <img src=\"foo.png\" /></p></body></html>";
        Document doc = Jsoup.parse(html);
        // need a better way to verify these:
        Element p = doc.getBody().child(0);
        assertEquals("p", p.tagName());
        Element img = p.child(0);
        assertEquals("foo.png", img.attr("src"));
        assertEquals("img", img.tagName());
    }

    @Test public void parsesRoughAttributes() {
        String html = "<html><head><title>First!</title></head><body><p class=\"foo > bar\">First post! <img src=\"foo.png\" /></p></body></html>";
        Document doc = Jsoup.parse(html);

        // need a better way to verify these:
        Element p = doc.getBody().child(0);
        assertEquals("p", p.tagName());
        assertEquals("foo > bar", p.attr("class"));
    }

    @Test public void parsesComments() {
        String html = "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);
        
        Element body = doc.getBody();
        Comment comment = (Comment) body.childNode(1); // comment should not be sub of img, as it's an empty tag
        assertEquals(" <table><tr><td></table> ", comment.getData());
        Element p = body.child(1);
        TextNode text = (TextNode) p.childNode(0);
        assertEquals("Hello", text.getWholeText());
    }

    @Test public void parsesUnterminatedComments() {
        String html = "<p>Hello<!-- <tr><td>";
        Document doc = Jsoup.parse(html);
        Element p = doc.getElementsByTag("p").get(0);
        assertEquals("Hello", p.text());
        TextNode text = (TextNode) p.childNode(0);
        assertEquals("Hello", text.getWholeText());
        Comment comment = (Comment) p.childNode(1);
        assertEquals(" <tr><td>", comment.getData());
    }

    @Test public void parsesUnterminatedTag() {
        String h1 = "<p";
        Document doc = Jsoup.parse(h1);
        assertEquals(1, doc.getElementsByTag("p").size());

        String h2 = "<div id=1<p id='2'";
        doc = Jsoup.parse(h2);
        Element d = doc.getElementById("1");
        assertEquals(1, d.children().size());
        Element p = doc.getElementById("2");
        assertNotNull(p);
    }

    @Test public void parsesUnterminatedAttribute() {
        String h1 = "<p id=\"foo";
        Document doc = Jsoup.parse(h1);
        Element p = doc.getElementById("foo");
        assertNotNull(p);
        assertEquals("p", p.tagName());
    }

    @Test public void createsDocumentStructure() {
        String html = "<meta name=keywords /><link rel=stylesheet /><title>jsoup</title><p>Hello world</p>";
        Document doc = Jsoup.parse(html);
        Element head = doc.getHead();
        Element body = doc.getBody();

        assertEquals(1, doc.children().size()); // root node: contains html node
        assertEquals(2, doc.child(0).children().size()); // html node: head and body
        assertEquals(3, head.children().size());
        assertEquals(1, body.children().size());

        assertEquals("keywords", head.getElementsByTag("meta").get(0).attr("name"));
        assertEquals(0, body.getElementsByTag("meta").size());
        assertEquals("jsoup", doc.getTitle());
        assertEquals("Hello world", body.text());
        assertEquals("Hello world", body.children().get(0).text());
    }

    @Test public void handlesEscapedData() {
        String html = "<div title='Surf &amp; Turf'>Reef &amp; Beef</div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.getElementsByTag("div").get(0);

        assertEquals("Surf & Turf", div.attr("title"));
        assertEquals("Reef & Beef", div.text());
    }

    @Test public void handlesDataOnlyTags() {
        String t = "<textarea><p>Foo</p>\n<p>Bar</p></TEXTAREA>";
        List<Element> tels = Jsoup.parse(t).getElementsByTag("textarea");
        assertEquals("<p>Foo</p>\n<p>Bar</p>", tels.get(0).data());

        String s = "<p>Hello</p><script>Nope</script><p>There</p>";
        Document doc = Jsoup.parse(s);
        assertEquals("Hello There", doc.text());
        assertEquals("Nope", doc.data());
    }

    @Test public void createsImplicitLists() {
        String h = "<li>Point one<li>Point two";
        Document doc = Jsoup.parse(h);
        Elements ol = doc.select("ul"); // should have created a default ul.
        assertEquals(1, ol.size());
        assertEquals(2, ol.get(0).children().size());

        // no fiddling with non-implicit lists
        String h2 = "<ol><li><p>Point the first<li><p>Point the second";
        Document doc2 = Jsoup.parse(h2);

        assertEquals(0, doc2.select("ul").size());
        assertEquals(1, doc2.select("ol").size());
        assertEquals(2, doc2.select("ol li").size());
        assertEquals(2, doc2.select("ol li p").size());
        assertEquals(1, doc2.select("ol li").get(0).children().size()); // one p in first li
    }

    @Test public void createsImplicitTable() {
        String h = "<td>Hello<td><p>There<p>now";
        Document doc = Jsoup.parse(h);
        assertEquals("<table><tr><td>Hello</td><td><p>There</p><p>now</p></td></tr></table>", doc.getBody().html());
    }

    @Test public void handlesBaseTags() {
        String h = "<a href=1>#</a><base href='/2/'><a href='3'>#</a><base href='http://bar'><a href=4>#</a>";
        Document doc = Jsoup.parse(h, "http://foo/");
        assertEquals("http://foo/", doc.baseUri());

        Elements anchors = doc.getElementsByTag("a");
        assertEquals(3, anchors.size());

        assertEquals("http://foo/", anchors.get(0).baseUri());
        assertEquals("http://foo/2/", anchors.get(1).baseUri());
        assertEquals("http://bar", anchors.get(2).baseUri());

        assertEquals("http://foo/1", anchors.get(0).absUrl("href"));
        assertEquals("http://foo/2/3", anchors.get(1).absUrl("href"));
        assertEquals("http://bar/4", anchors.get(2).absUrl("href"));
    }

    @Test public void handlesCdata() {
        String h = "<div id=1><![CData[<html>\n<foo><&amp;]]></div>"; // "cdata" insensitive. the &amp; in there should remain literal
        Document doc = Jsoup.parse(h);
        Element div = doc.getElementById("1");
        assertEquals("<html>\n<foo><&amp;", div.text());
        assertEquals(0, div.children().size());
        assertEquals(1, div.childNodes().size()); // no elements, one text node
    }

    @Test public void handlesInvalidStartTags() {
        String h = "<div>Hello < There <&amp;></div>"; // parse to <div {#text=Hello < There <&>}>
        Document doc = Jsoup.parse(h);
        assertEquals("Hello < There <&>", doc.select("div").first().text());
    }
    
    @Test public void handlesUnknownTags() {
        String h = "<div><foo title=bar>Hello<foo title=qux>there</foo></div>";
        Document doc = Jsoup.parse(h);
        Elements foos = doc.select("foo");
        assertEquals(2, foos.size());
        assertEquals("bar", foos.first().attr("title"));
        assertEquals("qux", foos.last().attr("title"));
        assertEquals("there", foos.last().text());
    }


}
