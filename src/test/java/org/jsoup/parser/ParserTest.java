package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 Tests for the Parser

 @author Jonathan Hedley, jonathan@hedley.net */
public class ParserTest {

    @Test public void parsesSimpleDocument() {
        String html = "<html><head><title>First!</title></head><body><p>First post! <img src=\"foo.png\" /></p></body></html>";
        Document doc = Jsoup.parse(html);
        // need a better way to verify these:
        Element p = doc.child(1).child(0);
        assertEquals("p", p.tagName());
        Element img = p.child(0);
        assertEquals("foo.png", img.attr("src"));
        assertEquals("img", img.tagName());
    }

    @Test public void parsesRoughAttributes() {
        String html = "<html><head><title>First!</title></head><body><p class=\"foo > bar\">First post! <img src=\"foo.png\" /></p></body></html>";
        Document doc = Jsoup.parse(html);

        // need a better way to verify these:
        Element p = doc.child(1).child(0);
        assertEquals("p", p.tagName());
        assertEquals("foo > bar", p.attr("class"));
    }

    @Test public void parsesComments() {
        String html = "<html><head></head><body><!-- <table><tr><td></table> --><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);
        
        Element body = doc.child(1);
        Comment comment = (Comment) body.childNode(0);
        assertEquals(" <table><tr><td></table> ", comment.getData());
        Element p = body.child(0);
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

        assertEquals(2, doc.children().size());
        assertEquals(3, head.children().size());
        assertEquals(1, body.children().size());

        assertEquals("keywords", head.getElementsByTag("meta").get(0).attr("name"));
        assertEquals(0, body.getElementsByTag("meta").size());
        assertEquals("jsoup", doc.getTitle());
        assertEquals("Hello world", body.text());
        assertEquals("Hello world", body.children().get(0).text());
    }


}
