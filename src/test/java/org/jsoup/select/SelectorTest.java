package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 Tests that the selector selects correctly.

 @author Jonathan Hedley, jonathan@hedley.net */
public class SelectorTest {
    @Test public void testByTag() {
        Elements els = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>").select("div");
        assertEquals(3, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
        assertEquals("3", els.get(2).id());

        Elements none = Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>").select("span");
        assertEquals(0, none.size());
    }

    @Test public void testById() {
        Elements els = Jsoup.parse("<div><p id=foo>Hello</p><p id=foo>Foo two!</p></div>").select("#foo");
        assertEquals(1, els.size());
        assertEquals("Hello", els.get(0).text());

        Elements none = Jsoup.parse("<div id=1></div>").select("#foo");
        assertEquals(0, none.size());
    }

    @Test public void testByClass() {
        Elements els = Jsoup.parse("<p id=0 class='one two'><p id=1 class='one'><p id=2 class='two'>").select("p.one");
        assertEquals(2, els.size());
        assertEquals("0", els.get(0).id());
        assertEquals("1", els.get(1).id());

        Elements none = Jsoup.parse("<div class='one'></div>").select(".foo");
        assertEquals(0, none.size());

        Elements els2 = Jsoup.parse("<div class='one-two'></div>").select(".one-two");
        assertEquals(1, els2.size());
    }

    @Test public void testByAttribute() {
        String h = "<div Title=Foo /><div Title=Bar /><div Style=Qux /><div title=Bam /><div title=SLAM /><div />";
        Document doc = Jsoup.parse(h);

        Elements withTitle = doc.select("[title]");
        assertEquals(4, withTitle.size());

        Elements foo = doc.select("[title=foo]");
        assertEquals(1, foo.size());

        Elements not = doc.select("div[title!=bar]");
        assertEquals(5, not.size());
        assertEquals("Foo", not.first().attr("title"));

        Elements starts = doc.select("[title^=ba]");
        assertEquals(2, starts.size());
        assertEquals("Bar", starts.first().attr("title"));
        assertEquals("Bam", starts.last().attr("title"));

        Elements ends = doc.select("[title$=am]");
        assertEquals(2, ends.size());
        assertEquals("Bam", ends.first().attr("title"));
        assertEquals("SLAM", ends.last().attr("title"));

        Elements contains = doc.select("[title*=a]");
        assertEquals(3, contains.size());
        assertEquals("Bar", contains.first().attr("title"));
        assertEquals("SLAM", contains.last().attr("title"));
    }

    @Test public void testAllElements() {
        String h = "<div><p>Hello</p><p><b>there</b></p></div>";
        Document doc = Jsoup.parse(h);
        Elements allDoc = doc.select("*");
        Elements allUnderDiv = doc.select("div *");
        assertEquals(8, allDoc.size());
        assertEquals(3, allUnderDiv.size());
        assertEquals("p", allUnderDiv.first().tagName());
    }
    
    @Test public void testAllWithClass() {
        String h = "<p class=first>One<p class=first>Two<p>Three";
        Document doc = Jsoup.parse(h);
        Elements ps = doc.select("*.first");
        assertEquals(2, ps.size());
    }

    @Test public void testGroupOr() {
        String h = "<div title=foo /><div title=bar /><div /><p></p><img /><span title=qux>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select("p,div,[title]");

        assertEquals(5, els.size());
        assertEquals("p", els.get(0).tagName());
        assertEquals("div", els.get(1).tagName());
        assertEquals("foo", els.get(1).attr("title"));
        assertEquals("div", els.get(2).tagName());
        assertEquals("bar", els.get(2).attr("title"));
        assertEquals("div", els.get(3).tagName());
        assertTrue(els.get(3).attr("title").length() == 0); // missing attributes come back as empty string
        assertFalse(els.get(3).hasAttr("title"));
        assertEquals("span", els.get(4).tagName());
    }

    @Test public void testGroupOrAttribute() {
        String h = "<div id=1 /><div id=2 /><div title=foo /><div title=bar />";
        Elements els = Jsoup.parse(h).select("[id],[title=foo]");

        assertEquals(3, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
        assertEquals("foo", els.get(2).attr("title"));
    }

    @Test public void descendant() {
        String h = "<div class=head><p class=first>Hello</p><p>There</p></div><p>None</p>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select(".head p");
        assertEquals(2, els.size());
        assertEquals("Hello", els.get(0).text());
        assertEquals("There", els.get(1).text());

        Elements p = doc.select("p.first");
        assertEquals(1, p.size());
        assertEquals("Hello", p.get(0).text());

        Elements empty = doc.select("p .first"); // self, not descend, should not match
        assertEquals(0, empty.size());
    }
    
    @Test public void and() {
        String h = "<div id=1 class='foo bar' title=bar name=qux><p class=foo title=bar>Hello</p></div";
        Document doc = Jsoup.parse(h);
        
        Elements div = doc.select("div.foo");
        assertEquals(1, div.size());
        assertEquals("div", div.first().tagName());
        
        Elements p = doc.select("div .foo"); // space indicates like "div *.foo"
        assertEquals(1, p.size());
        assertEquals("p", p.first().tagName());
        
        Elements div2 = doc.select("div#1.foo.bar[title=bar][name=qux]"); // very specific!
        assertEquals(1, div2.size());
        assertEquals("div", div2.first().tagName());
        
        Elements p2 = doc.select("div *.foo"); // space indicates like "div *.foo"
        assertEquals(1, p2.size());
        assertEquals("p", p2.first().tagName());
    }

    @Test public void deeperDescendant() {
        String h = "<div class=head><p><span class=first>Hello</div><div class=head><p class=first><span>Another</span><p>Again</div>";
        Elements els = Jsoup.parse(h).select("div p .first");
        assertEquals(1, els.size());
        assertEquals("Hello", els.first().text());
        assertEquals("span", els.first().tagName());
    }

    @Test public void parentChildElement() {
        String h = "<div id=1><div id=2><div id = 3></div></div></div><div id=4></div>";
        Document doc = Jsoup.parse(h);

        Elements divs = doc.select("div > div");
        assertEquals(2, divs.size());
        assertEquals("2", divs.get(0).id()); // 2 is child of 1
        assertEquals("3", divs.get(1).id()); // 3 is child of 2

        Elements div2 = doc.select("div#1 > div");
        assertEquals(1, div2.size());
        assertEquals("2", div2.get(0).id());
    }
    
    @Test public void parentWithClassChild() {
        String h = "<h1 class=foo><a href=1 /></h1><h1 class=foo><a href=2 class=bar /></h1><h1><a href=3 /></h1>";
        Document doc = Jsoup.parse(h);
        
        Elements allAs = doc.select("h1 > a");
        assertEquals(3, allAs.size());
        assertEquals("a", allAs.first().tagName());
        
        Elements fooAs = doc.select("h1.foo > a");
        assertEquals(2, fooAs.size());
        assertEquals("a", fooAs.first().tagName());
        
        Elements barAs = doc.select("h1.foo > a.bar");
        assertEquals(1, barAs.size());
    }

    @Test public void parentChildStar() {
        String h = "<div id=1><p>Hello<p><b>there</b></p></div><div id=2><span>Hi</span></div>";
        Document doc = Jsoup.parse(h);
        Elements divChilds = doc.select("div > *");
        assertEquals(3, divChilds.size());
        assertEquals("p", divChilds.get(0).tagName());
        assertEquals("p", divChilds.get(1).tagName());
        assertEquals("span", divChilds.get(2).tagName());
    }
    
    @Test public void multiChildDescent() {
        String h = "<div id=foo><h1 class=bar><a href=http://example.com/>One</a></h1></div>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select("div#foo > h1.bar > a[href*=example]");
        assertEquals(1, els.size());
        assertEquals("a", els.first().tagName());
    }

    @Test public void caseInsensitive() {
        String h = "<dIv tItle=bAr><div>"; // mixed case so a simple toLowerCase() on value doesn't catch
        Document doc = Jsoup.parse(h);

        assertEquals(2, doc.select("DIV").size());
        assertEquals(1, doc.select("DIV[TITLE]").size());
        assertEquals(1, doc.select("DIV[TITLE=BAR]").size());
        assertEquals(0, doc.select("DIV[TITLE=BARBARELLA").size());
    }
    
    @Test public void adjacentSiblings() {
        String h = "<ol><li>One<li>Two<li>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("li + li");
        assertEquals(2, sibs.size());
        assertEquals("Two", sibs.get(0).text());
        assertEquals("Three", sibs.get(1).text());
    }
    
    @Test public void adjacentSiblingsWithId() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("li#1 + li#2");
        assertEquals(1, sibs.size());
        assertEquals("Two", sibs.get(0).text());
    }
    
    @Test public void notAdjacent() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("li#1 + li#3");
        assertEquals(0, sibs.size());
    }
    
    @Test public void mixCombinator() {
        String h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>";
        Document doc = Jsoup.parse(h);
        Elements sibs = doc.select("body > div.foo li + li");
        
        assertEquals(2, sibs.size());
        assertEquals("Two", sibs.get(0).text());
        assertEquals("Three", sibs.get(1).text());
    }
    
    @Test public void mixCombinatorGroup() {
        String h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select(".foo > ol, ol > li + li");
        
        assertEquals(3, els.size());
        assertEquals("ol", els.get(0).tagName());
        assertEquals("Two", els.get(1).text());
        assertEquals("Three", els.get(2).text());
    }
    
    @Test public void generalSiblings() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select("#1 ~ #3");
        assertEquals(1, els.size());
        assertEquals("Three", els.first().text());
    }
    
    // for http://github.com/jhy/jsoup/issues#issue/10
    @Test public void testCharactersInIdAndClass() {
        // using CSS spec for identifiers (id and class): a-z0-9, -, _. NOT . (which is OK in html spec, but not css)
        String h = "<div><p id='a1-foo_bar'>One</p><p class='b2-qux_bif'>Two</p></div>";
        Document doc = Jsoup.parse(h);
        
        Element el1 = doc.getElementById("a1-foo_bar");
        assertEquals("One", el1.text());
        Element el2 = doc.getElementsByClass("b2-qux_bif").first();
        assertEquals("Two", el2.text());
        
        Element el3 = doc.select("#a1-foo_bar").first();
        assertEquals("One", el3.text());
        Element el4 = doc.select(".b2-qux_bif").first();
        assertEquals("Two", el4.text());
    }
    
    // for http://github.com/jhy/jsoup/issues#issue/13
    @Test public void testSupportsLeadingCombinator() {
        String h = "<div><p><span>One</span><span>Two</span></p></div>";
        Document doc = Jsoup.parse(h);
        
        Element p = doc.select("div > p").first();
        Elements spans = p.select("> span");
        assertEquals(2, spans.size());
        assertEquals("One", spans.first().text());
        
        // make sure doesn't get nested
        h = "<div id=1><div id=2><div id=3></div></div></div>";
        doc = Jsoup.parse(h);
        Element div = doc.select("div").select(" > div").first();
        assertEquals("2", div.id());
    }
    
    @Test public void testPseudoLessThan() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:lt(2)");
        assertEquals(3, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Two", ps.get(1).text());
        assertEquals("Four", ps.get(2).text());
    }
    
    @Test public void testPseudoGreaterThan() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:gt(0)");
        assertEquals(2, ps.size());
        assertEquals("Two", ps.get(0).text());
        assertEquals("Three", ps.get(1).text());
    }
    
    @Test public void testPseudoEquals() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:eq(0)");
        assertEquals(2, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Four", ps.get(1).text());
        
        Elements ps2 = doc.select("div:eq(0) p:eq(0)");
        assertEquals(1, ps2.size());
        assertEquals("One", ps2.get(0).text());
        assertEquals("p", ps2.get(0).tagName());
    }
    
    @Test public void testPseudoBetween() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = doc.select("div p:gt(0):lt(2)");
        assertEquals(1, ps.size());
        assertEquals("Two", ps.get(0).text());
    }
    
    @Test public void testPseudoCombined() {
        Document doc = Jsoup.parse("<div class='foo'><p>One</p><p>Two</p></div><div><p>Three</p><p>Four</p></div>");
        Elements ps = doc.select("div.foo p:gt(0)");
        assertEquals(1, ps.size());
        assertEquals("Two", ps.get(0).text());
    }
}
