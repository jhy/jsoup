package org.jsoup.parser;

//package org.jsoup.parser;

import org.jsoup.parser.*;
import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.helper.StringUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 Tests for the Parser

p^-1

-2p^-2
 @author Jonathan Hedley, jonathan@hedley.net */
public class HtmlParserTest2 {
    @Test public void parseFragmentTest(){
//        Element img = p.child(0);
//        assertEquals("foo.png", img.attr("src"));
//        assertEquals("img", img.tagName());
        Element el = new Element("xmp");
        String html = "<ol><li>One</li></ol><p>Two</p>";
        List<Node> nodes = Parser.parseFragment(html, el, "http://example.com/");
        assertEquals(1, nodes.size()); 
        
        el = new Element("script");
        List<Node> nodes2 = Parser.parseFragment(html, el, "http://example.com/");
        assertEquals(1, nodes.size()); 
        
        el = new Element("noscript");
        List<Node> nodes3 = Parser.parseFragment(html, el, "http://example.com/");
        assertEquals(1, nodes.size()); 

        el = new Element("plaintext");
        List<Node> nodes4 = Parser.parseFragment(html, el, "http://example.com/");
        assertEquals(1, nodes.size()); 

        el = new Element("textarea");
        List<Node> nodes5 = Parser.parseFragment(html, el, "http://example.com/");
        assertEquals(1, nodes.size()); 

        el = new Element("form");
        List<Node> nodes6 = Parser.parseFragment(html, el, "http://example.com/");
        assertEquals(1, nodes.size()); 
//        assertEquals("html", nodes.get(0).nodeName());
//        assertEquals("<html> <head></head> <body> <ol> <li>One</li> </ol> <p>Two</p> </body> </html>", StringUtil.normaliseWhitespace(nodes.get(0).outerHtml()));
    	

    }
//    @Test public void noTableDirectInTable() {
//        Document doc = Jsoup.parse("<table> <tbody>One <tr><table><tr>Two</table> <table><tr>Three");
//        assertEquals("<table> <tbody><tr><td>One </td><td><table><tbody><tr><td>Two</td></tr></tbody></table> <table><tbody><tr><td>Three</td></tr></tbody></table></td></tr></tbody></table>",
//                TextUtil.stripNewlines(doc.body().html()));
//    }

    @Test public void parsesUnterminatedOptgroup() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
        Document doc = Jsoup.parse("<body><p><select><optgroup><option>One<option>Two</p><p>Three</p><optgroup>dfdsf<optgroup>fdfg</optgroup>");
        Elements options = doc.select("option");
        assertEquals(2, options.size());
        assertEquals("One", options.first().text());
        assertEquals("TwoThree", options.last().text());
    }
    @Test public void parsesEmptyOptgroup() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
        Document doc = Jsoup.parse("<body><p><select><optgroup><option>asds</optgroup>");
        Elements options = doc.select("option");
        assertEquals(1, options.size());
    }
    @Test public void parseOutsidedComment() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
        Document doc = Jsoup.parse("<body>dsfsadf</body><!-- comment -->");
        Elements els = doc.getAllElements();
        assertEquals(4, els.size());
    } 
    @Test public void parseInitial(){
        Document doc = null;
        Elements els = null;

        doc = Jsoup.parse(" <p>text</p>");
        els = doc.select("p");
        assertEquals(1, els.size());
    }
    @Test public void parseBeforeHtml(){
        Document doc = null;
        Elements els = null;

        doc = Jsoup.parse("<!-- comment -->");
        els = doc.select("html");
        assertEquals(1, els.size());


        doc = Jsoup.parse("</head><html></html>");
        els = doc.select("html");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<!-- comment --><html></html>");
        els = doc.select("html");
        assertEquals(1, els.size());
    }
    @Test public void parseBeforeHead(){
        Document doc = null;
        Elements els = null;

        doc = Jsoup.parse("<!-- comment --><head></head>");
        els = doc.select("html");
        assertEquals(1, els.size());


        doc = Jsoup.parse("<html><head></head>");
        els = doc.select("html");
        assertEquals(1, els.size());

    }
    @Test public void parseAfterBody(){
        Document doc = null;
        Elements els = null;

        doc = Jsoup.parse("<body></body><html></html>");
        els = doc.select("body");
        assertEquals(1, els.size());
    }
    @Test public void parseInHead(){
        Document doc = null;
        Elements els = null;

        doc = Jsoup.parse("<head> </head>");
        els = doc.select("head");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<head><!-- comment --></head>");
        els = doc.select("head");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<head><html></html></head>");
        els = doc.select("head");
        assertEquals(1, els.size());
    }
    @Test public void parseAfterFrame(){
        Document doc = null;
        Elements els = null;

        doc = Jsoup.parse("<frameset cols=\"200,*\"></frameset> ");
        els = doc.select("frameset");
        assertEquals(1, els.size());
        
        doc = Jsoup.parse("<frameset cols=\"200,*\"></frameset><!-- comment --> ");
        els = doc.select("frameset");
        assertEquals(1, els.size());
        
        doc = Jsoup.parse("<frameset cols=\"200,*\"></frameset><html></html>");
        els = doc.select("frameset");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<frameset cols=\"200,*\"></frameset></html>");
        els = doc.select("frameset");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<frameset cols=\"200,*\"></frameset><noframes></noframes>");
        els = doc.select("frameset");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<frameset cols=\"200,*\"></frameset> ");
        els = doc.select("frameset");
        assertEquals(1, els.size());
    }
    @Test public void parseInFrame() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
//        Document doc = Jsoup.parse("<frameset cols=\"200,*\"> <frame src=\"menu.html\"> <frame src=\"contents.html\"> </frameset>");
        Document doc = Jsoup.parse("<frameset cols=\"200,*\"> </frameset");
        Elements els = doc.select("frameset");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<frameset cols=\"200,*\"><!-- comment --></frameset");
        els = doc.select("frameset");
        assertEquals(1, els.size());

        
        doc = Jsoup.parse("<frameset cols=\"200,*\"><html></html></frameset");
        els = doc.select("frameset");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<frameset cols=\"200,*\"><html></html></frameset");
        els = doc.select("frameset");
        assertEquals(1, els.size());
        
        doc = Jsoup.parse("<frameset cols=\"200,*\"><html></html></frameset");
        els = doc.select("frameset");
        assertEquals(1, els.size());
        
        doc = Jsoup.parse("<frameset cols=\"200,*\"><frameset></frameset></frameset");
        els = doc.select("frameset");
        assertEquals(2, els.size());

        doc = Jsoup.parse("<frameset cols=\"200,*\"><frame></frame></frameset");
        els = doc.select("frameset");
        assertEquals(1, els.size());

        doc = Jsoup.parse("<frameset cols=\"200,*\"><noframes></noframes></frameset>");
        els = doc.select("frameset");
        assertEquals(1, els.size());

    } 
}

