package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.ng.SelectMatch;
import org.jsoup.select.ng.parser.Parser;
import org.junit.Test;

import junit.framework.TestCase;

public class ParserSelectorNG extends TestCase {
    @Test 
    public void testByTag() {
        Elements els = SelectMatch.match(Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>"), Parser.parse("div"));
        assertEquals(3, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
        assertEquals("3", els.get(2).id());

        Elements none = SelectMatch.match(Jsoup.parse("<div id=1><div id=2><p>Hello</p></div></div><div id=3>"), Parser.parse("span"));
        assertEquals(0, none.size());
    }

    @Test 
    public void testById() {
        Elements els = SelectMatch.match(Jsoup.parse("<div><p id=foo>Hello</p><p id=foo>Foo two!</p></div>"), Parser.parse("#foo"));
        assertEquals(2, els.size());
        assertEquals("Hello", els.get(0).text());

        Elements none = SelectMatch.match(Jsoup.parse("<div id=1></div>"), Parser.parse("#foo"));
        assertEquals(0, none.size());
    }

    @Test 
    public void testByClass() {
        Elements els = SelectMatch.match(Jsoup.parse("<p id=0 class='one two'><p id=1 class='one'><p id=2 class='two'>"), Parser.parse("p.one"));
        assertEquals(2, els.size());
        assertEquals("0", els.get(0).id());
        assertEquals("1", els.get(1).id());

        Elements none = SelectMatch.match(Jsoup.parse("<div class='one'></div>"), Parser.parse(".foo"));
        assertEquals(0, none.size());

        Elements els2 = SelectMatch.match(Jsoup.parse("<div class='one-two'></div>"), Parser.parse(".one-two"));
        assertEquals(1, els2.size());
    }

    @Test 
    public void testByAttribute() {
        String h = "<div Title=Foo /><div Title=Bar /><div Style=Qux /><div title=Bam /><div title=SLAM /><div />";
        Document doc = Jsoup.parse(h);

        Elements withTitle = SelectMatch.match(doc, Parser.parse("[title]"));
        assertEquals(4, withTitle.size());

        Elements foo = SelectMatch.match(doc, Parser.parse("[title=foo]"));
        assertEquals(1, foo.size());

        Elements not = SelectMatch.match(doc, Parser.parse("div[title!=bar]"));
        assertEquals(5, not.size());
        assertEquals("Foo", not.first().attr("title"));

        Elements starts = SelectMatch.match(doc, Parser.parse("[title^=ba]"));
        assertEquals(2, starts.size());
        assertEquals("Bar", starts.first().attr("title"));
        assertEquals("Bam", starts.last().attr("title"));

        Elements ends = SelectMatch.match(doc, Parser.parse("[title$=am]"));
        assertEquals(2, ends.size());
        assertEquals("Bam", ends.first().attr("title"));
        assertEquals("SLAM", ends.last().attr("title"));

        Elements contains = SelectMatch.match(doc, Parser.parse("[title*=a]"));
        assertEquals(3, contains.size());
        assertEquals("Bar", contains.first().attr("title"));
        assertEquals("SLAM", contains.last().attr("title"));
    }
    
    @Test 
    public void testNamespacedTag() {
        Document doc = Jsoup.parse("<div><abc:def id=1>Hello</abc:def></div> <abc:def class=bold id=2>There</abc:def>");
        Elements byTag = SelectMatch.match(doc, Parser.parse("abc|def"));
        assertEquals(2, byTag.size());
        assertEquals("1", byTag.first().id());
        assertEquals("2", byTag.last().id());
        
        Elements byAttr = SelectMatch.match(doc, Parser.parse(".bold"));
        assertEquals(1, byAttr.size());
        assertEquals("2", byAttr.last().id());
        
        Elements byTagAttr = SelectMatch.match(doc, Parser.parse("abc|def.bold"));
        assertEquals(1, byTagAttr.size());
        assertEquals("2", byTagAttr.last().id());
        
        Elements byContains = SelectMatch.match(doc, Parser.parse("abc|def:contains(e)"));
        assertEquals(2, byContains.size());
        assertEquals("1", byContains.first().id());
        assertEquals("2", byContains.last().id());
    }

    @Test 
    public void testByAttributeStarting() {
        Document doc = Jsoup.parse("<div id=1 data-name=jsoup>Hello</div><p data-val=5 id=2>There</p><p id=3>No</p>");
        Elements withData = SelectMatch.match(doc, Parser.parse("[^data-]"));
        assertEquals(2, withData.size());
        assertEquals("1", withData.first().id());
        assertEquals("2", withData.last().id());

        withData = SelectMatch.match(doc, Parser.parse("p[^data-]"));
        assertEquals(1, withData.size());
        assertEquals("2", withData.first().id());
    }
    
    @Test 
    public void testByAttributeRegex() {
        Document doc = Jsoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif><img></p>");
        Elements imgs = SelectMatch.match(doc, Parser.parse("img[src~=(?i)\\.(png|jpe?g)]"));
        assertEquals(3, imgs.size());
        assertEquals("1", imgs.get(0).id());
        assertEquals("2", imgs.get(1).id());
        assertEquals("3", imgs.get(2).id());
    }

    @Test 
    public void testByAttributeRegexCharacterClass() {
        Document doc = Jsoup.parse("<p><img src=foo.png id=1><img src=bar.jpg id=2><img src=qux.JPEG id=3><img src=old.gif id=4></p>");
        Elements imgs = SelectMatch.match(doc, Parser.parse("img[src~=[o]]"));
        assertEquals(2, imgs.size());
        assertEquals("1", imgs.get(0).id());
        assertEquals("4", imgs.get(1).id());
    }

    @Test 
    public void testAllElements() {
        String h = "<div><p>Hello</p><p><b>there</b></p></div>";
        Document doc = Jsoup.parse(h);
        Elements allDoc = SelectMatch.match(doc, Parser.parse("*"));
        Elements allUnderDiv = SelectMatch.match(doc, Parser.parse("div *"));
        assertEquals(8, allDoc.size());
        assertEquals(3, allUnderDiv.size());
        assertEquals("p", allUnderDiv.first().tagName());
    }
    
    @Test 
    public void testAllWithClass() {
        String h = "<p class=first>One<p class=first>Two<p>Three";
        Document doc = Jsoup.parse(h);
        Elements ps = SelectMatch.match(doc, Parser.parse("*.first"));
        assertEquals(2, ps.size());
    }

    @Test 
    public void testGroupOr() {
        String h = "<div title=foo /><div title=bar /><div /><p></p><img /><span title=qux>";
        Document doc = Jsoup.parse(h);
        Elements els = SelectMatch.match(doc, Parser.parse("p,div,[title]"));

        assertEquals(5, els.size());
        assertEquals("div", els.get(0).tagName());
        assertEquals("foo", els.get(0).attr("title"));
        assertEquals("div", els.get(1).tagName());
        assertEquals("bar", els.get(1).attr("title"));
        assertEquals("div", els.get(2).tagName());
        assertTrue(els.get(2).attr("title").length() == 0); // missing attributes come back as empty string
        assertFalse(els.get(2).hasAttr("title"));
        assertEquals("p", els.get(3).tagName());
        assertEquals("span", els.get(4).tagName());
    }

    @Test 
    public void testGroupOrAttribute() {
        String h = "<div id=1 /><div id=2 /><div title=foo /><div title=bar />";
        Elements els = SelectMatch.match(Jsoup.parse(h), Parser.parse("[id],[title=foo]"));

        assertEquals(3, els.size());
        assertEquals("1", els.get(0).id());
        assertEquals("2", els.get(1).id());
        assertEquals("foo", els.get(2).attr("title"));
    }

    @Test 
    public void testDescendant() {
        String h = "<div class=head><p class=first>Hello</p><p>There</p></div><p>None</p>";
        Document doc = Jsoup.parse(h);
        Elements els = SelectMatch.match(doc, Parser.parse(".head p"));
        assertEquals(2, els.size());
        assertEquals("Hello", els.get(0).text());
        assertEquals("There", els.get(1).text());

        Elements p = SelectMatch.match(doc, Parser.parse("p.first"));
        assertEquals(1, p.size());
        assertEquals("Hello", p.get(0).text());

        Elements empty = SelectMatch.match(doc, Parser.parse("p .first")); // self, not descend, should not match
        assertEquals(0, empty.size());
    }
    
    @Test 
    public void testAnd() {
        String h = "<div id=1 class='foo bar' title=bar name=qux><p class=foo title=bar>Hello</p></div";
        Document doc = Jsoup.parse(h);
        
        Elements div = SelectMatch.match(doc, Parser.parse("div.foo"));
        assertEquals(1, div.size());
        assertEquals("div", div.first().tagName());
        
        Elements p = SelectMatch.match(doc, Parser.parse("div .foo")); // space indicates like "div *.foo"
        assertEquals(1, p.size());
        assertEquals("p", p.first().tagName());
        
        Elements div2 = SelectMatch.match(doc, Parser.parse("div#1.foo.bar[title=bar][name=qux]")); // very specific!
        assertEquals(1, div2.size());
        assertEquals("div", div2.first().tagName());
        
        Elements p2 = SelectMatch.match(doc, Parser.parse("div *.foo")); // space indicates like "div *.foo"
        assertEquals(1, p2.size());
        assertEquals("p", p2.first().tagName());
    }

    @Test 
    public void testDescendant2() {
        String h = "<div class=head><p><span class=first>Hello</div><div class=head><p class=first><span>Another</span><p>Again</div>";
        Elements els = SelectMatch.match(Jsoup.parse(h), Parser.parse("div p .first"));
        assertEquals(1, els.size());
        assertEquals("Hello", els.first().text());
        assertEquals("span", els.first().tagName());
    }

    @Test 
    public void testParentChildElement() {
        String h = "<div id=1><div id=2><div id = 3></div></div></div><div id=4></div>";
        Document doc = Jsoup.parse(h);

        Elements divs = SelectMatch.match(doc, Parser.parse("div > div"));
        assertEquals(2, divs.size());
        assertEquals("2", divs.get(0).id()); // 2 is child of 1
        assertEquals("3", divs.get(1).id()); // 3 is child of 2

        Elements div2 = SelectMatch.match(doc, Parser.parse("div#1 > div"));
        assertEquals(1, div2.size());
        assertEquals("2", div2.get(0).id());
    }
    
    @Test 
    public void testParentWithClassChild() {
        String h = "<h1 class=foo><a href=1 /></h1><h1 class=foo><a href=2 class=bar /></h1><h1><a href=3 /></h1>";
        Document doc = Jsoup.parse(h);
        
        Elements allAs = SelectMatch.match(doc, Parser.parse("h1 > a"));
        assertEquals(3, allAs.size());
        assertEquals("a", allAs.first().tagName());
        
        Elements fooAs = SelectMatch.match(doc, Parser.parse("h1.foo > a"));
        assertEquals(2, fooAs.size());
        assertEquals("a", fooAs.first().tagName());
        
        Elements barAs = SelectMatch.match(doc, Parser.parse("h1.foo > a.bar"));
        assertEquals(1, barAs.size());
    }

    @Test 
    public void testParentChildStar() {
        String h = "<div id=1><p>Hello<p><b>there</b></p></div><div id=2><span>Hi</span></div>";
        Document doc = Jsoup.parse(h);
        Elements divChilds = SelectMatch.match(doc, Parser.parse("div > *"));
        assertEquals(3, divChilds.size());
        assertEquals("p", divChilds.get(0).tagName());
        assertEquals("p", divChilds.get(1).tagName());
        assertEquals("span", divChilds.get(2).tagName());
    }
    
    @Test 
    public void testMultiChildDescent() {
        String h = "<div id=foo><h1 class=bar><a href=http://example.com/>One</a></h1></div>";
        Document doc = Jsoup.parse(h);
        Elements els = SelectMatch.match(doc, Parser.parse("div#foo > h1.bar > a[href*=example]"));
        assertEquals(1, els.size());
        assertEquals("a", els.first().tagName());
    }

    @Test 
    public void testCaseInsensitive() {
        String h = "<dIv tItle=bAr><div>"; // mixed case so a simple toLowerCase() on value doesn't catch
        Document doc = Jsoup.parse(h);

        assertEquals(2, SelectMatch.match(doc, Parser.parse("DIV")).size());
        assertEquals(1, SelectMatch.match(doc, Parser.parse("DIV[TITLE]")).size());
        assertEquals(1, SelectMatch.match(doc, Parser.parse("DIV[TITLE=BAR]")).size());
        assertEquals(0, SelectMatch.match(doc, Parser.parse("DIV[TITLE=BARBARELLA")).size());
    }
    
    @Test 
    public void testAdjacentSiblings() {
        String h = "<ol><li>One<li>Two<li>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = SelectMatch.match(doc, Parser.parse("li + li"));
        assertEquals(2, sibs.size());
        assertEquals("Two", sibs.get(0).text());
        assertEquals("Three", sibs.get(1).text());
    }
    
    @Test 
    public void testAdjacentSiblingsWithId() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = SelectMatch.match(doc, Parser.parse("li#1 + li#2"));
        assertEquals(1, sibs.size());
        assertEquals("Two", sibs.get(0).text());
    }
    
    @Test 
    public void testNotAdjacent() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements sibs = SelectMatch.match(doc, Parser.parse("li#1 + li#3"));
        assertEquals(0, sibs.size());
    }
    
    @Test 
    public void testMixCombinator() {
        String h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>";
        Document doc = Jsoup.parse(h);
        Elements sibs = SelectMatch.match(doc, Parser.parse("body > div.foo li + li"));
        
        assertEquals(2, sibs.size());
        assertEquals("Two", sibs.get(0).text());
        assertEquals("Three", sibs.get(1).text());
    }
    
    @Test 
    public void testMixCombinatorGroup() {
        String h = "<div class=foo><ol><li>One<li>Two<li>Three</ol></div>";
        Document doc = Jsoup.parse(h);
        Elements els = SelectMatch.match(doc, Parser.parse(".foo > ol, ol > li + li"));
        
        assertEquals(3, els.size());
        assertEquals("ol", els.get(0).tagName());
        assertEquals("Two", els.get(1).text());
        assertEquals("Three", els.get(2).text());
    }
    
    @Test 
    public void testGeneralSiblings() {
        String h = "<ol><li id=1>One<li id=2>Two<li id=3>Three</ol>";
        Document doc = Jsoup.parse(h);
        Elements els = SelectMatch.match(doc, Parser.parse("#1 ~ #3"));
        assertEquals(1, els.size());
        assertEquals("Three", els.first().text());
    }
    
    // for http://github.com/jhy/jsoup/issues#issue/10
    @Test 
    public void testCharactersInIdAndClass() {
        // using CSS spec for identifiers (id and class): a-z0-9, -, _. NOT . (which is OK in html spec, but not css)
        String h = "<div><p id='a1-foo_bar'>One</p><p class='b2-qux_bif'>Two</p></div>";
        Document doc = Jsoup.parse(h);
        
        Element el1 = doc.getElementById("a1-foo_bar");
        assertEquals("One", el1.text());
        Element el2 = doc.getElementsByClass("b2-qux_bif").first();
        assertEquals("Two", el2.text());
        
        Element el3 = SelectMatch.match(doc, Parser.parse("#a1-foo_bar")).first();
        assertEquals("One", el3.text());
        Element el4 = SelectMatch.match(doc, Parser.parse(".b2-qux_bif")).first();
        assertEquals("Two", el4.text());
    }
    
    // for http://github.com/jhy/jsoup/issues#issue/13
    @Test 
    public void testSupportsLeadingCombinator() {
        String h = "<div><p><span>One</span><span>Two</span></p></div>";
        Document doc = Jsoup.parse(h);
        
        Element p = SelectMatch.match(doc, Parser.parse("div > p")).first();
        Elements spans = p.select("> span");
        assertEquals(2, spans.size());
        assertEquals("One", spans.first().text());
        
        // make sure doesn't get nested
        h = "<div id=1><div id=2><div id=3></div></div></div>";
        doc = Jsoup.parse(h);
        Element div = SelectMatch.match(SelectMatch.match(doc, Parser.parse("div")), Parser.parse(" > div")).first();
        assertEquals("2", div.id());
    }
    
    @Test public void testPseudoLessThan() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = SelectMatch.match(doc, Parser.parse("div p:lt(2)"));
        assertEquals(3, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Two", ps.get(1).text());
        assertEquals("Four", ps.get(2).text());
    }
    
    @Test public void testPseudoGreaterThan() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</p></div><div><p>Four</p>");
        Elements ps = SelectMatch.match(doc, Parser.parse("div p:gt(0)"));
        assertEquals(2, ps.size());
        assertEquals("Two", ps.get(0).text());
        assertEquals("Three", ps.get(1).text());
    }
    
    @Test public void testPseudoEquals() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = SelectMatch.match(doc, Parser.parse("div p:eq(0)"));
        assertEquals(2, ps.size());
        assertEquals("One", ps.get(0).text());
        assertEquals("Four", ps.get(1).text());
        
        Elements ps2 = SelectMatch.match(doc, Parser.parse("div:eq(0) p:eq(0)"));
        assertEquals(1, ps2.size());
        assertEquals("One", ps2.get(0).text());
        assertEquals("p", ps2.get(0).tagName());
    }
    
    @Test public void testPseudoBetween() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p><p>Three</>p></div><div><p>Four</p>");
        Elements ps = SelectMatch.match(doc, Parser.parse("div p:gt(0):lt(2)"));
        assertEquals(1, ps.size());
        assertEquals("Two", ps.get(0).text());
    }
    
    @Test public void testPseudoCombined() {
        Document doc = Jsoup.parse("<div class='foo'><p>One</p><p>Two</p></div><div><p>Three</p><p>Four</p></div>");
        Elements ps = SelectMatch.match(doc, Parser.parse("div.foo p:gt(0)"));
        assertEquals(1, ps.size());
        assertEquals("Two", ps.get(0).text());
    }

    @Test public void testPseudoHas() {
        Document doc = Jsoup.parse("<div id=0><p><span>Hello</span></p></div> <div id=1><span class=foo>There</span></div> <div id=2><p>Not</p></div>");

        Elements divs1 = SelectMatch.match(doc, Parser.parse("div:has(span)"));
        assertEquals(2, divs1.size());
        assertEquals("0", divs1.get(0).id());
        assertEquals("1", divs1.get(1).id());

        Elements divs2 = SelectMatch.match(doc, Parser.parse("div:has([class]"));
        assertEquals(1, divs2.size());
        assertEquals("1", divs2.get(0).id());

        Elements divs3 = SelectMatch.match(doc, Parser.parse("div:has(span, p)"));
        assertEquals(3, divs3.size());
        assertEquals("0", divs3.get(0).id());
        assertEquals("1", divs3.get(1).id());
        assertEquals("2", divs3.get(2).id());

        Elements els1 = SelectMatch.match(doc.body(), Parser.parse(":has(p)"));
        assertEquals(3, els1.size()); // body, div, dib
        assertEquals("body", els1.first().tagName());
        assertEquals("0", els1.get(1).id());
        assertEquals("2", els1.get(2).id());
    }

    @Test public void testNestedHas() {
        Document doc = Jsoup.parse("<div><p><span>One</span></p></div> <div><p>Two</p></div>");
        Elements divs = SelectMatch.match(doc, Parser.parse("div:has(p:has(span))"));
        assertEquals(1, divs.size());
        assertEquals("One", divs.first().text());

        // test matches in has
        divs = SelectMatch.match(doc, Parser.parse("div:has(p:matches((?i)two))"));
        assertEquals(1, divs.size());
        assertEquals("div", divs.first().tagName());
        assertEquals("Two", divs.first().text());

        // test contains in has
        divs = SelectMatch.match(doc, Parser.parse("div:has(p:contains(two))"));
        assertEquals(1, divs.size());
        assertEquals("div", divs.first().tagName());
        assertEquals("Two", divs.first().text());
    }
    
    @Test public void testPseudoContains() {
        Document doc = Jsoup.parse("<div><p>The Rain.</p> <p class=light>The <i>rain</i>.</p> <p>Rain, the.</p></div>");
        
        Elements ps1 = SelectMatch.match(doc, Parser.parse("p:contains(Rain)"));
        assertEquals(3, ps1.size());
        
        Elements ps2 = SelectMatch.match(doc, Parser.parse("p:contains(the rain)"));
        assertEquals(2, ps2.size());
        assertEquals("The Rain.", ps2.first().html());
        assertEquals("The <i>rain</i>.", ps2.last().html());
        
        Elements ps3 = SelectMatch.match(doc, Parser.parse("p:contains(the Rain):has(i)"));
        assertEquals(1, ps3.size());
        assertEquals("light", ps3.first().className());

        Elements ps4 = SelectMatch.match(doc, Parser.parse(".light:contains(rain)"));
        assertEquals(1, ps4.size());
        assertEquals("light", ps3.first().className());

        Elements ps5 = SelectMatch.match(doc, Parser.parse(":contains(rain)"));
        assertEquals(8, ps5.size()); // html, body, div,...
    }
    
    @Test public void testPsuedoContainsWithParentheses() {
        Document doc = Jsoup.parse("<div><p id=1>This (is good)</p><p id=2>This is bad)</p>");
        
        Elements ps1 = SelectMatch.match(doc, Parser.parse("p:contains(this (is good))"));
        assertEquals(1, ps1.size());
        assertEquals("1", ps1.first().id());
        
        Elements ps2 = SelectMatch.match(doc, Parser.parse("p:contains(this is bad\\))"));
        assertEquals(1, ps2.size());
        assertEquals("2", ps2.first().id());
    }
    
    @Test public void testContainsOwn() {
        Document doc = Jsoup.parse("<p id=1>Hello <b>there</b> now</p>");
        Elements ps = SelectMatch.match(doc, Parser.parse("p:containsOwn(Hello now)"));
        assertEquals(1, ps.size());
        assertEquals("1", ps.first().id());
        
        assertEquals(0, SelectMatch.match(doc, Parser.parse("p:containsOwn(there)")).size());
    }
    
    @Test public void testMatches() {       
        Document doc = Jsoup.parse("<p id=1>The <i>Rain</i></p> <p id=2>There are 99 bottles.</p> <p id=3>Harder (this)</p> <p id=4>Rain</p>");
        
        Elements p1 = SelectMatch.match(doc, Parser.parse("p:matches(The rain)")); // no match, case sensitive
        assertEquals(0, p1.size());
        
        Elements p2 = SelectMatch.match(doc, Parser.parse("p:matches((?i)the rain)")); // case insense. should include root, html, body
        assertEquals(1, p2.size());
        assertEquals("1", p2.first().id());
        
        Elements p4 = SelectMatch.match(doc, Parser.parse("p:matches((?i)^rain$)")); // bounding
        assertEquals(1, p4.size());
        assertEquals("4", p4.first().id());
        
        Elements p5 = SelectMatch.match(doc, Parser.parse("p:matches(\\d+)"));
        assertEquals(1, p5.size());
        assertEquals("2", p5.first().id());
        
        Elements p6 = SelectMatch.match(doc, Parser.parse("p:matches(\\w+\\s+\\(\\w+\\))")); // test bracket matching
        assertEquals(1, p6.size());
        assertEquals("3", p6.first().id());
        
        Elements p7 = SelectMatch.match(doc, Parser.parse("p:matches((?i)the):has(i)")); // multi
        assertEquals(1, p7.size());
        assertEquals("1", p7.first().id());
    }
    
    @Test public void testMatchesOwn() {
        Document doc = Jsoup.parse("<p id=1>Hello <b>there</b> now</p>");
        
        Elements p1 = SelectMatch.match(doc, Parser.parse("p:matchesOwn((?i)hello now)"));
        assertEquals(1, p1.size());
        assertEquals("1", p1.first().id());
        
        assertEquals(0, SelectMatch.match(doc, Parser.parse("p:matchesOwn(there)")).size());
    }
    
    @Test public void testRelaxedTags() {
        Document doc = Jsoup.parse("<abc_def id=1>Hello</abc_def> <abc-def id=2>There</abc-def>");
        
        Elements el1 = SelectMatch.match(doc, Parser.parse("abc_def"));
        assertEquals(1, el1.size());
        assertEquals("1", el1.first().id());
        
        Elements el2 = SelectMatch.match(doc, Parser.parse("abc-def"));
        assertEquals(1, el2.size());
        assertEquals("2", el2.first().id());
    }

    @Test public void notParas() {
        Document doc = Jsoup.parse("<p id=1>One</p> <p>Two</p> <p><span>Three</span></p>");

        Elements el1 = SelectMatch.match(doc, Parser.parse("p:not([id=1])"));
        assertEquals(2, el1.size());
        assertEquals("Two", el1.first().text());
        assertEquals("Three", el1.last().text());

        Elements el2 = SelectMatch.match(doc, Parser.parse("p:not(:has(span))"));
        assertEquals(2, el2.size());
        assertEquals("One", el2.first().text());
        assertEquals("Two", el2.last().text());
    }

    @Test 
    public void testNotAll() {
        Document doc = Jsoup.parse("<p>Two</p> <p><span>Three</span></p>");

        Elements el1 = doc.body().select(":not(p)"); // should just be the span
        assertEquals(2, el1.size());
        assertEquals("body", el1.first().tagName());
        assertEquals("span", el1.last().tagName());
    }

    @Test 
    public void testNotClass() {
        Document doc = Jsoup.parse("<div class=left>One</div><div class=right id=1><p>Two</p></div>");

        Elements el1 = SelectMatch.match(doc, Parser.parse("div:not(.left)"));
        assertEquals(1, el1.size());
        assertEquals("1", el1.first().id());
    }

	

}
