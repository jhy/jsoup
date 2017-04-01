package testoracle;

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
    @Test public void ksooTest(){
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
    @Test public void noTableDirectInTable() {
        Document doc = Jsoup.parse("<table> <tbody>One <tr><table><tr>Two</table> <table><tr>Three");
        assertEquals("<table> <tbody><tr><td>One </td><td><table><tbody><tr><td>Two</td></tr></tbody></table> <table><tbody><tr><td>Three</td></tr></tbody></table></td></tr></tbody></table>",
                TextUtil.stripNewlines(doc.body().html()));
    }

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
    @Test public void parseTest() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
        Document doc = Jsoup.parse("<table><tr><td><select>sdfasdf");
        Elements options = doc.select("option");
        assertEquals(1, options.size());
    }
    @Test public void parseOutsidedComment() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
        Document doc = Jsoup.parse("<body>dsfsadf</body><!-- comment -->");
        Elements els = doc.getAllElements();
        assertEquals(4, els.size());
    } 
}

