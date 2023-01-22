package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.integration.ParseTest;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.*;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.jsoup.parser.ParseSettings.preserveCase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Parser
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class HtmlParserTest {

    @Test public void parsesSimpleDocument() {
        String html = "<html><head><title>First!</title></head><body><p>First post! <img src=\"foo.png\" /></p></body></html>";
        Document doc = Jsoup.parse(html);
        // need a better way to verify these:
        Element p = doc.body().child(0);
        assertEquals("p", p.tagName());
        Element img = p.child(0);
        assertEquals("foo.png", img.attr("src"));
        assertEquals("img", img.tagName());
    }

    @Test public void parsesRoughAttributes() {
        String html = "<html><head><title>First!</title></head><body><p class=\"foo > bar\">First post! <img src=\"foo.png\" /></p></body></html>";
        Document doc = Jsoup.parse(html);

        // need a better way to verify these:
        Element p = doc.body().child(0);
        assertEquals("p", p.tagName());
        assertEquals("foo > bar", p.attr("class"));
    }

    @Test public void dropsDuplicateAttributes() {
        String html = "<p One=One ONE=Two Two=two one=Three One=Four two=Five>Text</p>";
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document doc = parser.parseInput(html, "");

        Element p = doc.selectFirst("p");
        assertEquals("<p one=\"One\" two=\"two\">Text</p>", p.outerHtml()); // normalized names due to lower casing

        assertEquals(1, parser.getErrors().size());
        assertEquals("Dropped duplicate attribute(s) in tag [p]", parser.getErrors().get(0).getErrorMessage());
    }

    @Test public void retainsAttributesOfDifferentCaseIfSensitive() {
        String html = "<p One=One One=Two one=Three two=Four two=Five Two=Six>Text</p>";
        Parser parser = Parser.htmlParser().settings(preserveCase);
        Document doc = parser.parseInput(html, "");
        assertEquals("<p One=\"One\" one=\"Three\" two=\"Four\" Two=\"Six\">Text</p>", doc.selectFirst("p").outerHtml());
    }

    @Test public void parsesQuiteRoughAttributes() {
        String html = "<p =a>One<a <p>Something</p>Else";
        // this (used to; now gets cleaner) gets a <p> with attr '=a' and an <a tag with an attribute named '<p'; and then auto-recreated
        Document doc = Jsoup.parse(html);

        // NOTE: per spec this should be the test case. but impacts too many ppl
        // assertEquals("<p =a>One<a <p>Something</a></p>\n<a <p>Else</a>", doc.body().html());

        assertEquals("<p a>One<a></a></p><p><a>Something</a></p><a>Else</a>", TextUtil.stripNewlines(doc.body().html()));

        doc = Jsoup.parse("<p .....>");
        assertEquals("<p .....></p>", doc.body().html());
    }

    @Test public void parsesComments() {
        String html = "<html><head></head><body><img src=foo><!-- <table><tr><td></table> --><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);

        Element body = doc.body();
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

    @Test void allDashCommentsAreNotParseErrors() {
        // https://github.com/jhy/jsoup/issues/1667
        // <!-----> is not a parse error
        String html = "<!------>";
        Parser parser = Parser.htmlParser().setTrackErrors(10);
        Document doc = Jsoup.parse(html, parser);
        Comment comment = (Comment) doc.childNode(0);
        assertEquals("--", comment.getData());
        assertEquals(0, parser.getErrors().size());
    }

    @Test public void dropsUnterminatedTag() {
        // jsoup used to parse this to <p>, but whatwg, webkit will drop.
        String h1 = "<p";
        Document doc = Jsoup.parse(h1);
        assertEquals(0, doc.getElementsByTag("p").size());
        assertEquals("", doc.text());

        String h2 = "<div id=1<p id='2'";
        doc = Jsoup.parse(h2);
        assertEquals("", doc.text());
    }

    @Test public void dropsUnterminatedAttribute() {
        // jsoup used to parse this to <p id="foo">, but whatwg, webkit will drop.
        String h1 = "<p id=\"foo";
        Document doc = Jsoup.parse(h1);
        assertEquals("", doc.text());
    }

    @Test public void parsesUnterminatedTextarea() {
        // don't parse right to end, but break on <p>
        Document doc = Jsoup.parse("<body><p><textarea>one<p>two");
        Element t = doc.select("textarea").first();
        assertEquals("one", t.text());
        assertEquals("two", doc.select("p").get(1).text());
    }

    @Test public void parsesUnterminatedOption() {
        // bit weird this -- browsers and spec get stuck in select until there's a </select>
        Document doc = Jsoup.parse("<body><p><select><option>One<option>Two</p><p>Three</p>");
        Elements options = doc.select("option");
        assertEquals(2, options.size());
        assertEquals("One", options.first().text());
        assertEquals("TwoThree", options.last().text());
    }

    @Test public void testSelectWithOption() {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(10);
        Document document = parser.parseInput("<select><option>Option 1</option></select>", "http://jsoup.org");
        assertEquals(0, parser.getErrors().size());
    }

    @Test public void testSpaceAfterTag() {
        Document doc = Jsoup.parse("<div > <a name=\"top\"></a ><p id=1 >Hello</p></div>");
        assertEquals("<div><a name=\"top\"></a><p id=\"1\">Hello</p></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void createsDocumentStructure() {
        String html = "<meta name=keywords /><link rel=stylesheet /><title>jsoup</title><p>Hello world</p>";
        Document doc = Jsoup.parse(html);
        Element head = doc.head();
        Element body = doc.body();

        assertEquals(1, doc.children().size()); // root node: contains html node
        assertEquals(2, doc.child(0).children().size()); // html node: head and body
        assertEquals(3, head.children().size());
        assertEquals(1, body.children().size());

        assertEquals("keywords", head.getElementsByTag("meta").get(0).attr("name"));
        assertEquals(0, body.getElementsByTag("meta").size());
        assertEquals("jsoup", doc.title());
        assertEquals("Hello world", body.text());
        assertEquals("Hello world", body.children().get(0).text());
    }

    @Test public void createsStructureFromBodySnippet() {
        // the bar baz stuff naturally goes into the body, but the 'foo' goes into root, and the normalisation routine
        // needs to move into the start of the body
        String html = "foo <b>bar</b> baz";
        Document doc = Jsoup.parse(html);
        assertEquals("foo bar baz", doc.text());
    }

    @Test public void handlesEscapedData() {
        String html = "<div title='Surf &amp; Turf'>Reef &amp; Beef</div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.getElementsByTag("div").get(0);

        assertEquals("Surf & Turf", div.attr("title"));
        assertEquals("Reef & Beef", div.text());
    }

    @Test public void handlesDataOnlyTags() {
        String t = "<style>font-family: bold</style>";
        List<Element> tels = Jsoup.parse(t).getElementsByTag("style");
        assertEquals("font-family: bold", tels.get(0).data());
        assertEquals("", tels.get(0).text());

        String s = "<p>Hello</p><script>obj.insert('<a rel=\"none\" />');\ni++;</script><p>There</p>";
        Document doc = Jsoup.parse(s);
        assertEquals("Hello There", doc.text());
        assertEquals("obj.insert('<a rel=\"none\" />');\ni++;", doc.data());
    }

    @Test public void handlesTextAfterData() {
        String h = "<html><body>pre <script>inner</script> aft</body></html>";
        Document doc = Jsoup.parse(h);
        assertEquals("<html><head></head><body>pre <script>inner</script> aft</body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void handlesTextArea() {
        Document doc = Jsoup.parse("<textarea>Hello</textarea>");
        Elements els = doc.select("textarea");
        assertEquals("Hello", els.text());
        assertEquals("Hello", els.val());
    }

    @Test public void preservesSpaceInTextArea() {
        // preserve because the tag is marked as preserve white space
        Document doc = Jsoup.parse("<textarea>\n\tOne\n\tTwo\n\tThree\n</textarea>");
        String expect = "One\n\tTwo\n\tThree"; // the leading and trailing spaces are dropped as a convenience to authors
        Element el = doc.select("textarea").first();
        assertEquals(expect, el.text());
        assertEquals(expect, el.val());
        assertEquals(expect, el.html());
        assertEquals("<textarea>\n\t" + expect + "\n</textarea>", el.outerHtml()); // but preserved in round-trip html
    }

    @Test public void preservesSpaceInScript() {
        // preserve because it's content is a data node
        Document doc = Jsoup.parse("<script>\nOne\n\tTwo\n\tThree\n</script>");
        String expect = "\nOne\n\tTwo\n\tThree\n";
        Element el = doc.select("script").first();
        assertEquals(expect, el.data());
        assertEquals("One\n\tTwo\n\tThree", el.html());
        assertEquals("<script>" + expect + "</script>", el.outerHtml());
    }

    @Test public void doesNotCreateImplicitLists() {
        // old jsoup used to wrap this in <ul>, but that's not to spec
        String h = "<li>Point one<li>Point two";
        Document doc = Jsoup.parse(h);
        Elements ol = doc.select("ul"); // should NOT have created a default ul.
        assertEquals(0, ol.size());
        Elements lis = doc.select("li");
        assertEquals(2, lis.size());
        assertEquals("body", lis.first().parent().tagName());

        // no fiddling with non-implicit lists
        String h2 = "<ol><li><p>Point the first<li><p>Point the second";
        Document doc2 = Jsoup.parse(h2);

        assertEquals(0, doc2.select("ul").size());
        assertEquals(1, doc2.select("ol").size());
        assertEquals(2, doc2.select("ol li").size());
        assertEquals(2, doc2.select("ol li p").size());
        assertEquals(1, doc2.select("ol li").get(0).children().size()); // one p in first li
    }

    @Test public void discardsNakedTds() {
        // jsoup used to make this into an implicit table; but browsers make it into a text run
        String h = "<td>Hello<td><p>There<p>now";
        Document doc = Jsoup.parse(h);
        assertEquals("Hello<p>There</p><p>now</p>", TextUtil.stripNewlines(doc.body().html()));
        // <tbody> is introduced if no implicitly creating table, but allows tr to be directly under table
    }

    @Test public void handlesNestedImplicitTable() {
        Document doc = Jsoup.parse("<table><td>1</td></tr> <td>2</td></tr> <td> <table><td>3</td> <td>4</td></table> <tr><td>5</table>");
        assertEquals("<table><tbody><tr><td>1</td></tr><tr><td>2</td></tr><tr><td><table><tbody><tr><td>3</td><td>4</td></tr></tbody></table></td></tr><tr><td>5</td></tr></tbody></table>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesWhatWgExpensesTableExample() {
        // http://www.whatwg.org/specs/web-apps/current-work/multipage/tabular-data.html#examples-0
        Document doc = Jsoup.parse("<table> <colgroup> <col> <colgroup> <col> <col> <col> <thead> <tr> <th> <th>2008 <th>2007 <th>2006 <tbody> <tr> <th scope=rowgroup> Research and development <td> $ 1,109 <td> $ 782 <td> $ 712 <tr> <th scope=row> Percentage of net sales <td> 3.4% <td> 3.3% <td> 3.7% <tbody> <tr> <th scope=rowgroup> Selling, general, and administrative <td> $ 3,761 <td> $ 2,963 <td> $ 2,433 <tr> <th scope=row> Percentage of net sales <td> 11.6% <td> 12.3% <td> 12.6% </table>");
        assertEquals("<table><colgroup><col></colgroup><colgroup><col><col><col></colgroup><thead><tr><th></th><th>2008</th><th>2007</th><th>2006</th></tr></thead><tbody><tr><th scope=\"rowgroup\">Research and development</th><td>$ 1,109</td><td>$ 782</td><td>$ 712</td></tr><tr><th scope=\"row\">Percentage of net sales</th><td>3.4%</td><td>3.3%</td><td>3.7%</td></tr></tbody><tbody><tr><th scope=\"rowgroup\">Selling, general, and administrative</th><td>$ 3,761</td><td>$ 2,963</td><td>$ 2,433</td></tr><tr><th scope=\"row\">Percentage of net sales</th><td>11.6%</td><td>12.3%</td><td>12.6%</td></tr></tbody></table>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesTbodyTable() {
        Document doc = Jsoup.parse("<html><head></head><body><table><tbody><tr><td>aaa</td><td>bbb</td></tr></tbody></table></body></html>");
        assertEquals("<table><tbody><tr><td>aaa</td><td>bbb</td></tr></tbody></table>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesImplicitCaptionClose() {
        Document doc = Jsoup.parse("<table><caption>A caption<td>One<td>Two");
        assertEquals("<table><caption>A caption</caption><tbody><tr><td>One</td><td>Two</td></tr></tbody></table>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void noTableDirectInTable() {
        Document doc = Jsoup.parse("<table> <td>One <td><table><td>Two</table> <table><td>Three");
        assertEquals("<table><tbody><tr><td>One</td><td><table><tbody><tr><td>Two</td></tr></tbody></table><table><tbody><tr><td>Three</td></tr></tbody></table></td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void ignoresDupeEndTrTag() {
        Document doc = Jsoup.parse("<table><tr><td>One</td><td><table><tr><td>Two</td></tr></tr></table></td><td>Three</td></tr></table>"); // two </tr></tr>, must ignore or will close table
        assertEquals("<table><tbody><tr><td>One</td><td><table><tbody><tr><td>Two</td></tr></tbody></table></td><td>Three</td></tr></tbody></table>",
            TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesBaseTags() {
        // only listen to the first base href
        String h = "<a href=1>#</a><base href='/2/'><a href='3'>#</a><base href='http://bar'><a href=/4>#</a>";
        Document doc = Jsoup.parse(h, "http://foo/");
        assertEquals("http://foo/2/", doc.baseUri()); // gets set once, so doc and descendants have first only

        Elements anchors = doc.getElementsByTag("a");
        assertEquals(3, anchors.size());

        assertEquals("http://foo/2/", anchors.get(0).baseUri());
        assertEquals("http://foo/2/", anchors.get(1).baseUri());
        assertEquals("http://foo/2/", anchors.get(2).baseUri());

        assertEquals("http://foo/2/1", anchors.get(0).absUrl("href"));
        assertEquals("http://foo/2/3", anchors.get(1).absUrl("href"));
        assertEquals("http://foo/4", anchors.get(2).absUrl("href"));
    }

    @Test public void handlesProtocolRelativeUrl() {
        String base = "https://example.com/";
        String html = "<img src='//example.net/img.jpg'>";
        Document doc = Jsoup.parse(html, base);
        Element el = doc.select("img").first();
        assertEquals("https://example.net/img.jpg", el.absUrl("src"));
    }

    @Test public void handlesCdata() {
        // todo: as this is html namespace, should actually treat as bogus comment, not cdata. keep as cdata for now
        String h = "<div id=1><![CDATA[<html>\n <foo><&amp;]]></div>"; // the &amp; in there should remain literal
        Document doc = Jsoup.parse(h);
        Element div = doc.getElementById("1");
        assertEquals("<html>\n <foo><&amp;", div.text());
        assertEquals(0, div.children().size());
        assertEquals(1, div.childNodeSize()); // no elements, one text node
    }

    @Test public void roundTripsCdata() {
        String h = "<div id=1><![CDATA[\n<html>\n <foo><&amp;]]></div>";
        Document doc = Jsoup.parse(h);
        Element div = doc.getElementById("1");
        assertEquals("<html>\n <foo><&amp;", div.text());
        assertEquals(0, div.children().size());
        assertEquals(1, div.childNodeSize()); // no elements, one text node

        assertEquals("<div id=\"1\"><![CDATA[\n<html>\n <foo><&amp;]]>\n</div>", div.outerHtml());

        CDataNode cdata = (CDataNode) div.textNodes().get(0);
        assertEquals("\n<html>\n <foo><&amp;", cdata.text());
    }

    @Test public void handlesCdataAcrossBuffer() {
        StringBuilder sb = new StringBuilder();
        while (sb.length() <= CharacterReader.maxBufferLen) {
            sb.append("A suitable amount of CData.\n");
        }
        String cdata = sb.toString();
        String h = "<div><![CDATA[" + cdata + "]]></div>";
        Document doc = Jsoup.parse(h);
        Element div = doc.selectFirst("div");

        CDataNode node = (CDataNode) div.textNodes().get(0);
        assertEquals(cdata, node.text());
    }

    @Test public void handlesCdataInScript() {
        String html = "<script type=\"text/javascript\">//<![CDATA[\n\n  foo();\n//]]></script>";
        Document doc = Jsoup.parse(html);

        String data = "//<![CDATA[\n\n  foo();\n//]]>";
        Element script = doc.selectFirst("script");
        assertEquals("", script.text()); // won't be parsed as cdata because in script data section
        assertEquals(data, script.data());
        assertEquals(html, script.outerHtml());

        DataNode dataNode = (DataNode) script.childNode(0);
        assertEquals(data, dataNode.getWholeData());
        // see - not a cdata node, because in script. contrast with XmlTreeBuilder - will be cdata.
    }

    @Test public void handlesUnclosedCdataAtEOF() {
        // https://github.com/jhy/jsoup/issues/349 would crash, as character reader would try to seek past EOF
        String h = "<![CDATA[]]";
        Document doc = Jsoup.parse(h);
        assertEquals(1, doc.body().childNodeSize());
    }

    @Test public void handleCDataInText() {
        String h = "<p>One <![CDATA[Two <&]]> Three</p>";
        Document doc = Jsoup.parse(h);
        Element p = doc.selectFirst("p");

        List<Node> nodes = p.childNodes();
        assertEquals("One ", ((TextNode) nodes.get(0)).getWholeText());
        assertEquals("Two <&", ((TextNode) nodes.get(1)).getWholeText());
        assertEquals("Two <&", ((CDataNode) nodes.get(1)).getWholeText());
        assertEquals(" Three", ((TextNode) nodes.get(2)).getWholeText());

        assertEquals(h, p.outerHtml());
    }

    @Test public void cdataNodesAreTextNodes() {
        String h = "<p>One <![CDATA[ Two <& ]]> Three</p>";
        Document doc = Jsoup.parse(h);
        Element p = doc.selectFirst("p");

        List<TextNode> nodes = p.textNodes();
        assertEquals("One ", nodes.get(0).text());
        assertEquals(" Two <& ", nodes.get(1).text());
        assertEquals(" Three", nodes.get(2).text());
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

    @Test public void handlesUnknownInlineTags() {
        String h = "<p><cust>Test</cust></p><p><cust><cust>Test</cust></cust></p>";
        Document doc = Jsoup.parseBodyFragment(h);
        String out = doc.body().html();
        assertEquals(h, TextUtil.stripNewlines(out));
    }

    @Test public void parsesBodyFragment() {
        String h = "<!-- comment --><p><a href='foo'>One</a></p>";
        Document doc = Jsoup.parseBodyFragment(h, "http://example.com");
        assertEquals("<body><!-- comment --><p><a href=\"foo\">One</a></p></body>", TextUtil.stripNewlines(doc.body().outerHtml()));
        assertEquals("http://example.com/foo", doc.select("a").first().absUrl("href"));
    }

    @Test public void parseBodyIsIndexNoAttributes() {
        // https://github.com/jhy/jsoup/issues/1404
        String expectedHtml = "<form>\n" +
            " <hr><label>This is a searchable index. Enter search keywords: <input name=\"isindex\"></label>\n" +
            " <hr>\n" +
            "</form>";
        Document doc = Jsoup.parse("<isindex>");
        assertEquals(expectedHtml, doc.body().html());

        doc = Jsoup.parseBodyFragment("<isindex>");
        assertEquals(expectedHtml, doc.body().html());

        doc = Jsoup.parseBodyFragment("<table><input></table>");
        assertEquals("<input>\n<table></table>", doc.body().html());
    }

    @Test public void handlesUnknownNamespaceTags() {
        // note that the first foo:bar should not really be allowed to be self closing, if parsed in html mode.
        String h = "<foo:bar id='1' /><abc:def id=2>Foo<p>Hello</p></abc:def><foo:bar>There</foo:bar>";
        Document doc = Jsoup.parse(h);
        assertEquals("<foo:bar id=\"1\" /><abc:def id=\"2\">Foo<p>Hello</p></abc:def><foo:bar>There</foo:bar>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesKnownEmptyBlocks() {
        // if a known tag, allow self closing outside of spec, but force an end tag. unknown tags can be self closing.
        String h = "<div id='1' /><script src='/foo' /><div id=2><img /><img></div><a id=3 /><i /><foo /><foo>One</foo> <hr /> hr text <hr> hr text two";
        Document doc = Jsoup.parse(h);
        assertEquals("<div id=\"1\"></div><script src=\"/foo\"></script><div id=\"2\"><img><img></div><a id=\"3\"></a><i></i><foo /><foo>One</foo><hr> hr text <hr> hr text two", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesKnownEmptyNoFrames() {
        String h = "<html><head><noframes /><meta name=foo></head><body>One</body></html>";
        Document doc = Jsoup.parse(h);
        assertEquals("<html><head><noframes></noframes><meta name=\"foo\"></head><body>One</body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void handlesKnownEmptyStyle() {
        String h = "<html><head><style /><meta name=foo></head><body>One</body></html>";
        Document doc = Jsoup.parse(h);
        assertEquals("<html><head><style></style><meta name=\"foo\"></head><body>One</body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void handlesKnownEmptyTitle() {
        String h = "<html><head><title /><meta name=foo></head><body>One</body></html>";
        Document doc = Jsoup.parse(h);
        assertEquals("<html><head><title></title><meta name=\"foo\"></head><body>One</body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void handlesKnownEmptyIframe() {
        String h = "<p>One</p><iframe id=1 /><p>Two";
        Document doc = Jsoup.parse(h);
        assertEquals("<html><head></head><body><p>One</p><iframe id=\"1\"></iframe><p>Two</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void handlesSolidusAtAttributeEnd() {
        // this test makes sure [<a href=/>link</a>] is parsed as [<a href="/">link</a>], not [<a href="" /><a>link</a>]
        String h = "<a href=/>link</a>";
        Document doc = Jsoup.parse(h);
        assertEquals("<a href=\"/\">link</a>", doc.body().html());
    }

    @Test public void handlesMultiClosingBody() {
        String h = "<body><p>Hello</body><p>there</p></body></body></html><p>now";
        Document doc = Jsoup.parse(h);
        assertEquals(3, doc.select("p").size());
        assertEquals(3, doc.body().children().size());
    }

    @Test public void handlesUnclosedDefinitionLists() {
        // jsoup used to create a <dl>, but that's not to spec
        String h = "<dt>Foo<dd>Bar<dt>Qux<dd>Zug";
        Document doc = Jsoup.parse(h);
        assertEquals(0, doc.select("dl").size()); // no auto dl
        assertEquals(4, doc.select("dt, dd").size());
        Elements dts = doc.select("dt");
        assertEquals(2, dts.size());
        assertEquals("Zug", dts.get(1).nextElementSibling().text());
    }

    @Test public void handlesBlocksInDefinitions() {
        // per the spec, dt and dd are inline, but in practise are block
        String h = "<dl><dt><div id=1>Term</div></dt><dd><div id=2>Def</div></dd></dl>";
        Document doc = Jsoup.parse(h);
        assertEquals("dt", doc.select("#1").first().parent().tagName());
        assertEquals("dd", doc.select("#2").first().parent().tagName());
        assertEquals("<dl><dt><div id=\"1\">Term</div></dt><dd><div id=\"2\">Def</div></dd></dl>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesFrames() {
        String h = "<html><head><script></script><noscript></noscript></head><frameset><frame src=foo></frame><frame src=foo></frameset></html>";
        Document doc = Jsoup.parse(h);
        assertEquals("<html><head><script></script><noscript></noscript></head><frameset><frame src=\"foo\"><frame src=\"foo\"></frameset></html>",
            TextUtil.stripNewlines(doc.html()));
        // no body auto vivification
    }

    @Test public void ignoresContentAfterFrameset() {
        String h = "<html><head><title>One</title></head><frameset><frame /><frame /></frameset><table></table></html>";
        Document doc = Jsoup.parse(h);
        assertEquals("<html><head><title>One</title></head><frameset><frame><frame></frameset></html>", TextUtil.stripNewlines(doc.html()));
        // no body, no table. No crash!
    }

    @Test public void handlesJavadocFont() {
        String h = "<TD BGCOLOR=\"#EEEEFF\" CLASS=\"NavBarCell1\">    <A HREF=\"deprecated-list.html\"><FONT CLASS=\"NavBarFont1\"><B>Deprecated</B></FONT></A>&nbsp;</TD>";
        Document doc = Jsoup.parse(h);
        Element a = doc.select("a").first();
        assertEquals("Deprecated", a.text());
        assertEquals("font", a.child(0).tagName());
        assertEquals("b", a.child(0).child(0).tagName());
    }

    @Test public void handlesBaseWithoutHref() {
        String h = "<head><base target='_blank'></head><body><a href=/foo>Test</a></body>";
        Document doc = Jsoup.parse(h, "http://example.com/");
        Element a = doc.select("a").first();
        assertEquals("/foo", a.attr("href"));
        assertEquals("http://example.com/foo", a.attr("abs:href"));
    }

    @Test public void normalisesDocument() {
        String h = "<!doctype html>One<html>Two<head>Three<link></head>Four<body>Five </body>Six </html>Seven ";
        Document doc = Jsoup.parse(h);
        assertEquals("<!doctype html><html><head></head><body>OneTwoThree<link>FourFive Six Seven</body></html>",
            TextUtil.stripNewlines(doc.html()));
    }

    @Test public void normalisesEmptyDocument() {
        Document doc = Jsoup.parse("");
        assertEquals("<html><head></head><body></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void normalisesHeadlessBody() {
        Document doc = Jsoup.parse("<html><body><span class=\"foo\">bar</span>");
        assertEquals("<html><head></head><body><span class=\"foo\">bar</span></body></html>",
            TextUtil.stripNewlines(doc.html()));
    }

    @Test public void normalisedBodyAfterContent() {
        Document doc = Jsoup.parse("<font face=Arial><body class=name><div>One</div></body></font>");
        assertEquals("<html><head></head><body class=\"name\"><font face=\"Arial\"><div>One</div></font></body></html>",
            TextUtil.stripNewlines(doc.html()));
    }

    @Test public void findsCharsetInMalformedMeta() {
        String h = "<meta http-equiv=Content-Type content=text/html; charset=gb2312>";
        // example cited for reason of html5's <meta charset> element
        Document doc = Jsoup.parse(h);
        assertEquals("gb2312", doc.select("meta").attr("charset"));
    }

    @Test public void testHgroup() {
        // jsoup used to not allow hgroup in h{n}, but that's not in spec, and browsers are OK
        Document doc = Jsoup.parse("<h1>Hello <h2>There <hgroup><h1>Another<h2>headline</hgroup> <hgroup><h1>More</h1><p>stuff</p></hgroup>");
        assertEquals("<h1>Hello</h1><h2>There <hgroup><h1>Another</h1><h2>headline</h2></hgroup><hgroup><h1>More</h1><p>stuff</p></hgroup></h2>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void testRelaxedTags() {
        Document doc = Jsoup.parse("<abc_def id=1>Hello</abc_def> <abc-def>There</abc-def>");
        assertEquals("<abc_def id=\"1\">Hello</abc_def> <abc-def>There</abc-def>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void testHeaderContents() {
        // h* tags (h1 .. h9) in browsers can handle any internal content other than other h*. which is not per any
        // spec, which defines them as containing phrasing content only. so, reality over theory.
        Document doc = Jsoup.parse("<h1>Hello <div>There</div> now</h1> <h2>More <h3>Content</h3></h2>");
        assertEquals("<h1>Hello <div>There</div> now</h1><h2>More</h2><h3>Content</h3>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void testSpanContents() {
        // like h1 tags, the spec says SPAN is phrasing only, but browsers and publisher treat span as a block tag
        Document doc = Jsoup.parse("<span>Hello <div>there</div> <span>now</span></span>");
        assertEquals("<span>Hello <div>there</div><span>now</span></span>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void testNoImagesInNoScriptInHead() {
        // jsoup used to allow, but against spec if parsing with noscript
        Document doc = Jsoup.parse("<html><head><noscript><img src='foo'></noscript></head><body><p>Hello</p></body></html>");
        assertEquals("<html><head><noscript>&lt;img src=\"foo\"&gt;</noscript></head><body><p>Hello</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void testUnclosedNoscriptInHead() {
        // Was getting "EOF" in html output, because the #anythingElse handler was calling an undefined toString, so used object.toString.
        String[] strings = {"<noscript>", "<noscript>One"};
        for (String html : strings) {
            Document doc = Jsoup.parse(html);
            assertEquals(html + "</noscript>", TextUtil.stripNewlines(doc.head().html()));
        }
    }

    @Test public void testAFlowContents() {
        // html5 has <a> as either phrasing or block
        Document doc = Jsoup.parse("<a>Hello <div>there</div> <span>now</span></a>");
        assertEquals("<a>Hello <div>there</div><span>now</span></a>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void testFontFlowContents() {
        // html5 has no definition of <font>; often used as flow
        Document doc = Jsoup.parse("<font>Hello <div>there</div> <span>now</span></font>");
        assertEquals("<font>Hello <div>there</div><span>now</span></font>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesMisnestedTagsBI() {
        // whatwg: <b><i></b></i>
        String h = "<p>1<b>2<i>3</b>4</i>5</p>";
        Document doc = Jsoup.parse(h);
        assertEquals("<p>1<b>2<i>3</i></b><i>4</i>5</p>", doc.body().html());
        // adoption agency on </b>, reconstruction of formatters on 4.
    }

    @Test public void handlesMisnestedTagsBP() {
        //  whatwg: <b><p></b></p>
        String h = "<b>1<p>2</b>3</p>";
        Document doc = Jsoup.parse(h);
        assertEquals("<b>1</b>\n<p><b>2</b>3</p>", doc.body().html());
    }

    @Test public void handlesMisnestedAInDivs() {
        String h = "<a href='#1'><div><div><a href='#2'>child</a></div</div></a>";
        String w = "<a href=\"#1\"></a> <div> <a href=\"#1\"></a> <div> <a href=\"#1\"></a><a href=\"#2\">child</a> </div> </div>";
        Document doc = Jsoup.parse(h);
        assertEquals(
            StringUtil.normaliseWhitespace(w),
            StringUtil.normaliseWhitespace(doc.body().html()));
    }

    @Test public void handlesUnexpectedMarkupInTables() {
        // whatwg - tests markers in active formatting (if they didn't work, would get in table)
        // also tests foster parenting
        String h = "<table><b><tr><td>aaa</td></tr>bbb</table>ccc";
        Document doc = Jsoup.parse(h);
        assertEquals("<b></b><b>bbb</b><table><tbody><tr><td>aaa</td></tr></tbody></table><b>ccc</b>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesUnclosedFormattingElements() {
        // whatwg: formatting elements get collected and applied, but excess elements are thrown away
        String h = "<!DOCTYPE html>\n" +
            "<p><b class=x><b class=x><b><b class=x><b class=x><b>X\n" +
            "<p>X\n" +
            "<p><b><b class=x><b>X\n" +
            "<p></b></b></b></b></b></b>X";
        Document doc = Jsoup.parse(h);
        doc.outputSettings().indentAmount(0);
        String want = "<!doctype html>\n" +
            "<html>\n" +
            "<head></head>\n" +
            "<body>\n" +
            "<p><b class=\"x\"><b class=\"x\"><b><b class=\"x\"><b class=\"x\"><b>X </b></b></b></b></b></b></p>\n" +
            "<p><b class=\"x\"><b><b class=\"x\"><b class=\"x\"><b>X </b></b></b></b></b></p>\n" +
            "<p><b class=\"x\"><b><b class=\"x\"><b class=\"x\"><b><b><b class=\"x\"><b>X </b></b></b></b></b></b></b></b></p>\n" +
            "<p>X</p>\n" +
            "</body>\n" +
            "</html>";
        assertEquals(want, doc.html());
    }

    @Test public void handlesUnclosedAnchors() {
        String h = "<a href='http://example.com/'>Link<p>Error link</a>";
        Document doc = Jsoup.parse(h);
        String want = "<a href=\"http://example.com/\">Link</a>\n<p><a href=\"http://example.com/\">Error link</a></p>";
        assertEquals(want, doc.body().html());
    }

    @Test public void reconstructFormattingElements() {
        // tests attributes and multi b
        String h = "<p><b class=one>One <i>Two <b>Three</p><p>Hello</p>";
        Document doc = Jsoup.parse(h);
        assertEquals("<p><b class=\"one\">One <i>Two <b>Three</b></i></b></p>\n<p><b class=\"one\"><i><b>Hello</b></i></b></p>", doc.body().html());
    }

    @Test public void reconstructFormattingElementsInTable() {
        // tests that tables get formatting markers -- the <b> applies outside the table and does not leak in,
        // and the <i> inside the table and does not leak out.
        String h = "<p><b>One</p> <table><tr><td><p><i>Three<p>Four</i></td></tr></table> <p>Five</p>";
        Document doc = Jsoup.parse(h);
        String want = "<p><b>One</b></p><b>\n" +
            " <table>\n" +
            "  <tbody>\n" +
            "   <tr>\n" +
            "    <td><p><i>Three</i></p><p><i>Four</i></p></td>\n" +
            "   </tr>\n" +
            "  </tbody>\n" +
            " </table><p>Five</p></b>";
        assertEquals(want, doc.body().html());
    }

    @Test public void commentBeforeHtml() {
        String h = "<!-- comment --><!-- comment 2 --><p>One</p>";
        Document doc = Jsoup.parse(h);
        assertEquals("<!-- comment --><!-- comment 2 --><html><head></head><body><p>One</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test public void emptyTdTag() {
        String h = "<table><tr><td>One</td><td id='2' /></tr></table>";
        Document doc = Jsoup.parse(h);
        assertEquals("<td>One</td>\n<td id=\"2\"></td>", doc.select("tr").first().html());
    }

    @Test public void handlesSolidusInA() {
        // test for bug #66
        String h = "<a class=lp href=/lib/14160711/>link text</a>";
        Document doc = Jsoup.parse(h);
        Element a = doc.select("a").first();
        assertEquals("link text", a.text());
        assertEquals("/lib/14160711/", a.attr("href"));
    }

    @Test public void handlesSpanInTbody() {
        // test for bug 64
        String h = "<table><tbody><span class='1'><tr><td>One</td></tr><tr><td>Two</td></tr></span></tbody></table>";
        Document doc = Jsoup.parse(h);
        assertEquals(doc.select("span").first().children().size(), 0); // the span gets closed
        assertEquals(doc.select("table").size(), 1); // only one table
    }

    @Test public void handlesUnclosedTitleAtEof() {
        assertEquals("Data", Jsoup.parse("<title>Data").title());
        assertEquals("Data<", Jsoup.parse("<title>Data<").title());
        assertEquals("Data</", Jsoup.parse("<title>Data</").title());
        assertEquals("Data</t", Jsoup.parse("<title>Data</t").title());
        assertEquals("Data</ti", Jsoup.parse("<title>Data</ti").title());
        assertEquals("Data", Jsoup.parse("<title>Data</title>").title());
        assertEquals("Data", Jsoup.parse("<title>Data</title >").title());
    }

    @Test public void handlesUnclosedTitle() {
        Document one = Jsoup.parse("<title>One <b>Two <b>Three</TITLE><p>Test</p>"); // has title, so <b> is plain text
        assertEquals("One <b>Two <b>Three", one.title());
        assertEquals("Test", one.select("p").first().text());

        Document two = Jsoup.parse("<title>One<b>Two <p>Test</p>"); // no title, so <b> causes </title> breakout
        assertEquals("One", two.title());
        assertEquals("<b>Two <p>Test</p></b>", two.body().html());
    }

    @Test public void handlesUnclosedScriptAtEof() {
        assertEquals("Data", Jsoup.parse("<script>Data").select("script").first().data());
        assertEquals("Data<", Jsoup.parse("<script>Data<").select("script").first().data());
        assertEquals("Data</sc", Jsoup.parse("<script>Data</sc").select("script").first().data());
        assertEquals("Data</-sc", Jsoup.parse("<script>Data</-sc").select("script").first().data());
        assertEquals("Data</sc-", Jsoup.parse("<script>Data</sc-").select("script").first().data());
        assertEquals("Data</sc--", Jsoup.parse("<script>Data</sc--").select("script").first().data());
        assertEquals("Data", Jsoup.parse("<script>Data</script>").select("script").first().data());
        assertEquals("Data</script", Jsoup.parse("<script>Data</script").select("script").first().data());
        assertEquals("Data", Jsoup.parse("<script>Data</script ").select("script").first().data());
        assertEquals("Data", Jsoup.parse("<script>Data</script n").select("script").first().data());
        assertEquals("Data", Jsoup.parse("<script>Data</script n=").select("script").first().data());
        assertEquals("Data", Jsoup.parse("<script>Data</script n=\"").select("script").first().data());
        assertEquals("Data", Jsoup.parse("<script>Data</script n=\"p").select("script").first().data());
    }

    @Test public void handlesUnclosedRawtextAtEof() {
        assertEquals("Data", Jsoup.parse("<style>Data").select("style").first().data());
        assertEquals("Data</st", Jsoup.parse("<style>Data</st").select("style").first().data());
        assertEquals("Data", Jsoup.parse("<style>Data</style>").select("style").first().data());
        assertEquals("Data</style", Jsoup.parse("<style>Data</style").select("style").first().data());
        assertEquals("Data</-style", Jsoup.parse("<style>Data</-style").select("style").first().data());
        assertEquals("Data</style-", Jsoup.parse("<style>Data</style-").select("style").first().data());
        assertEquals("Data</style--", Jsoup.parse("<style>Data</style--").select("style").first().data());
    }

    @Test public void noImplicitFormForTextAreas() {
        // old jsoup parser would create implicit forms for form children like <textarea>, but no more
        Document doc = Jsoup.parse("<textarea>One</textarea>");
        assertEquals("<textarea>One</textarea>", doc.body().html());
    }

    @Test public void handlesEscapedScript() {
        Document doc = Jsoup.parse("<script><!-- one <script>Blah</script> --></script>");
        assertEquals("<!-- one <script>Blah</script> -->", doc.select("script").first().data());
    }

    @Test public void handles0CharacterAsText() {
        Document doc = Jsoup.parse("0<p>0</p>");
        assertEquals("0\n<p>0</p>", doc.body().html());
    }

    @Test public void handlesNullInData() {
        Document doc = Jsoup.parse("<p id=\u0000>Blah \u0000</p>");
        assertEquals("<p id=\"\uFFFD\">Blah &#x0;</p>", doc.body().html()); // replaced in attr, NOT replaced in data (but is escaped as control char <0x20)
    }

    @Test public void handlesNullInComments() {
        Document doc = Jsoup.parse("<body><!-- \u0000 \u0000 -->");
        assertEquals("<!-- \uFFFD \uFFFD -->", doc.body().html());
    }

    @Test public void handlesNewlinesAndWhitespaceInTag() {
        Document doc = Jsoup.parse("<a \n href=\"one\" \r\n id=\"two\" \f >");
        assertEquals("<a href=\"one\" id=\"two\"></a>", doc.body().html());
    }

    @Test public void handlesWhitespaceInoDocType() {
        String html = "<!DOCTYPE html\r\n" +
            "      PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\r\n" +
            "      \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        Document doc = Jsoup.parse(html);
        assertEquals("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">", doc.childNode(0).outerHtml());
    }

    @Test public void tracksErrorsWhenRequested() {
        String html = "<p>One</p href='no'>\n<!DOCTYPE html>\n&arrgh;<font />&#33 &amp &#xD800;<br /></div><foo";
        Parser parser = Parser.htmlParser().setTrackErrors(500);
        Document doc = Jsoup.parse(html, "http://example.com", parser);

        List<ParseError> errors = parser.getErrors();
        assertEquals(9, errors.size());
        assertEquals("<1:21>: Attributes incorrectly present on end tag [/p]", errors.get(0).toString());
        assertEquals("<2:16>: Unexpected Doctype token [<!doctype html>] when in state [InBody]", errors.get(1).toString());
        assertEquals("<3:2>: Invalid character reference: invalid named reference [arrgh]", errors.get(2).toString());
        assertEquals("<3:16>: Tag [font] cannot be self closing; not a void tag", errors.get(3).toString());
        assertEquals("<3:20>: Invalid character reference: missing semicolon on [&#33]", errors.get(4).toString());
        assertEquals("<3:25>: Invalid character reference: missing semicolon on [&amp]", errors.get(5).toString());
        assertEquals("<3:34>: Invalid character reference: character [55296] outside of valid range", errors.get(6).toString());
        assertEquals("<3:46>: Unexpected EndTag token [</div>] when in state [InBody]", errors.get(7).toString());
        assertEquals("<3:51>: Unexpectedly reached end of file (EOF) in input state [TagName]", errors.get(8).toString());
    }

    @Test public void tracksLimitedErrorsWhenRequested() {
        String html = "<p>One</p href='no'>\n<!DOCTYPE html>\n&arrgh;<font /><br /><foo";
        Parser parser = Parser.htmlParser().setTrackErrors(3);
        Document doc = parser.parseInput(html, "http://example.com");

        List<ParseError> errors = parser.getErrors();
        assertEquals(3, errors.size());
        assertEquals("<1:21>: Attributes incorrectly present on end tag [/p]", errors.get(0).toString());
        assertEquals("<2:16>: Unexpected Doctype token [<!doctype html>] when in state [InBody]", errors.get(1).toString());
        assertEquals("<3:2>: Invalid character reference: invalid named reference [arrgh]", errors.get(2).toString());
    }

    @Test public void noErrorsByDefault() {
        String html = "<p>One</p href='no'>&arrgh;<font /><br /><foo";
        Parser parser = Parser.htmlParser();
        Document doc = Jsoup.parse(html, "http://example.com", parser);

        List<ParseError> errors = parser.getErrors();
        assertEquals(0, errors.size());
    }

    @Test public void optionalPClosersAreNotErrors() {
        String html = "<body><div><p>One<p>Two</div></body>";
        Parser parser = Parser.htmlParser().setTrackErrors(128);
        Document doc = Jsoup.parse(html, "", parser);
        ParseErrorList errors = parser.getErrors();
        assertEquals(0, errors.size());
    }

    @Test public void handlesCommentsInTable() {
        String html = "<table><tr><td>text</td><!-- Comment --></tr></table>";
        Document node = Jsoup.parseBodyFragment(html);
        assertEquals("<html><head></head><body><table><tbody><tr><td>text</td><!-- Comment --></tr></tbody></table></body></html>", TextUtil.stripNewlines(node.outerHtml()));
    }

    @Test public void handlesQuotesInCommentsInScripts() {
        String html = "<script>\n" +
            "  <!--\n" +
            "    document.write('</scr' + 'ipt>');\n" +
            "  // -->\n" +
            "</script>";
        Document node = Jsoup.parseBodyFragment(html);
        assertEquals("<script>\n" +
            "  <!--\n" +
            "    document.write('</scr' + 'ipt>');\n" +
            "  // -->\n" +
            "</script>", node.body().html());
    }

    @Test public void handleNullContextInParseFragment() {
        String html = "<ol><li>One</li></ol><p>Two</p>";
        List<Node> nodes = Parser.parseFragment(html, null, "http://example.com/");
        assertEquals(1, nodes.size()); // returns <html> node (not document) -- no context means doc gets created
        assertEquals("html", nodes.get(0).nodeName());
        assertEquals("<html> <head></head> <body> <ol> <li>One</li> </ol> <p>Two</p> </body> </html>", StringUtil.normaliseWhitespace(nodes.get(0).outerHtml()));
    }

    @Test public void doesNotFindShortestMatchingEntity() {
        // previous behaviour was to identify a possible entity, then chomp down the string until a match was found.
        // (as defined in html5.) However in practise that lead to spurious matches against the author's intent.
        String html = "One &clubsuite; &clubsuit;";
        Document doc = Jsoup.parse(html);
        assertEquals(StringUtil.normaliseWhitespace("One &amp;clubsuite; ♣"), doc.body().html());
    }

    @Test public void relaxedBaseEntityMatchAndStrictExtendedMatch() {
        // extended entities need a ; at the end to match, base does not
        String html = "&amp &quot &reg &icy &hopf &icy; &hopf;";
        Document doc = Jsoup.parse(html);
        doc.outputSettings().escapeMode(Entities.EscapeMode.extended).charset("ascii"); // modifies output only to clarify test
        assertEquals("&amp; \" &reg; &amp;icy &amp;hopf &icy; &hopf;", doc.body().html());
    }

    @Test public void handlesXmlDeclarationAsBogusComment() {
        String html = "<?xml encoding='UTF-8' ?><body>One</body>";
        Document doc = Jsoup.parse(html);
        assertEquals("<!--?xml encoding='UTF-8' ?--> <html> <head></head> <body> One </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));
    }

    @Test public void handlesTagsInTextarea() {
        String html = "<textarea><p>Jsoup</p></textarea>";
        Document doc = Jsoup.parse(html);
        assertEquals("<textarea>&lt;p&gt;Jsoup&lt;/p&gt;</textarea>", doc.body().html());
    }

    // form tests
    @Test public void createsFormElements() {
        String html = "<body><form><input id=1><input id=2></form></body>";
        Document doc = Jsoup.parse(html);
        Element el = doc.select("form").first();

        assertTrue(el instanceof FormElement, "Is form element");
        FormElement form = (FormElement) el;
        Elements controls = form.elements();
        assertEquals(2, controls.size());
        assertEquals("1", controls.get(0).id());
        assertEquals("2", controls.get(1).id());
    }

    @Test public void associatedFormControlsWithDisjointForms() {
        // form gets closed, isn't parent of controls
        String html = "<table><tr><form><input type=hidden id=1><td><input type=text id=2></td><tr></table>";
        Document doc = Jsoup.parse(html);
        Element el = doc.select("form").first();

        assertTrue(el instanceof FormElement, "Is form element");
        FormElement form = (FormElement) el;
        Elements controls = form.elements();
        assertEquals(2, controls.size());
        assertEquals("1", controls.get(0).id());
        assertEquals("2", controls.get(1).id());

        assertEquals("<table><tbody><tr><form></form><input type=\"hidden\" id=\"1\"><td><input type=\"text\" id=\"2\"></td></tr><tr></tr></tbody></table>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void handlesInputInTable() {
        String h = "<body>\n" +
            "<input type=\"hidden\" name=\"a\" value=\"\">\n" +
            "<table>\n" +
            "<input type=\"hidden\" name=\"b\" value=\"\" />\n" +
            "</table>\n" +
            "</body>";
        Document doc = Jsoup.parse(h);
        assertEquals(1, doc.select("table input").size());
        assertEquals(2, doc.select("input").size());
    }

    @Test public void convertsImageToImg() {
        // image to img, unless in a svg. old html cruft.
        String h = "<body><image><svg><image /></svg></body>";
        Document doc = Jsoup.parse(h);
        assertEquals("<img>\n<svg>\n <image />\n</svg>", doc.body().html());
    }

    @Test public void handlesInvalidDoctypes() {
        // would previously throw invalid name exception on empty doctype
        Document doc = Jsoup.parse("<!DOCTYPE>");
        assertEquals(
            "<!doctype> <html> <head></head> <body></body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()));

        doc = Jsoup.parse("<!DOCTYPE><html><p>Foo</p></html>");
        assertEquals(
            "<!doctype> <html> <head></head> <body> <p>Foo</p> </body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()));

        doc = Jsoup.parse("<!DOCTYPE \u0000>");
        assertEquals(
            "<!doctype �> <html> <head></head> <body></body> </html>",
            StringUtil.normaliseWhitespace(doc.outerHtml()));
    }

    @Test public void handlesManyChildren() {
        // Arrange
        StringBuilder longBody = new StringBuilder(500000);
        for (int i = 0; i < 25000; i++) {
            longBody.append(i).append("<br>");
        }

        // Act
        long start = System.currentTimeMillis();
        Document doc = Parser.parseBodyFragment(longBody.toString(), "");

        // Assert
        assertEquals(50000, doc.body().childNodeSize());
        assertTrue(System.currentTimeMillis() - start < 1000);
    }

    @Test
    public void testInvalidTableContents() throws IOException {
        File in = ParseTest.getFile("/htmltests/table-invalid-elements.html");
        Document doc = Jsoup.parse(in, "UTF-8");
        doc.outputSettings().prettyPrint(true);
        String rendered = doc.toString();
        int endOfEmail = rendered.indexOf("Comment");
        int guarantee = rendered.indexOf("Why am I here?");
        assertTrue(endOfEmail > -1, "Comment not found");
        assertTrue(guarantee > -1, "Search text not found");
        assertTrue(guarantee > endOfEmail, "Search text did not come after comment");
    }

    @Test public void testNormalisesIsIndex() {
        Document doc = Jsoup.parse("<body><isindex action='/submit'></body>");
        String html = doc.outerHtml();
        assertEquals("<form action=\"/submit\"> <hr><label>This is a searchable index. Enter search keywords: <input name=\"isindex\"></label> <hr> </form>",
            StringUtil.normaliseWhitespace(doc.body().html()));
    }

    @Test public void testReinsertionModeForThCelss() {
        String body = "<body> <table> <tr> <th> <table><tr><td></td></tr></table> <div> <table><tr><td></td></tr></table> </div> <div></div> <div></div> <div></div> </th> </tr> </table> </body>";
        Document doc = Jsoup.parse(body);
        assertEquals(1, doc.body().children().size());
    }

    @Test public void testUsingSingleQuotesInQueries() {
        String body = "<body> <div class='main'>hello</div></body>";
        Document doc = Jsoup.parse(body);
        Elements main = doc.select("div[class='main']");
        assertEquals("hello", main.text());
    }

    @Test public void testSupportsNonAsciiTags() {
        String body = "<a進捗推移グラフ>Yes</a進捗推移グラフ><bрусский-тэг>Correct</<bрусский-тэг>";
        Document doc = Jsoup.parse(body);
        Elements els = doc.select("a進捗推移グラフ");
        assertEquals("Yes", els.text());
        els = doc.select("bрусский-тэг");
        assertEquals("Correct", els.text());
    }

    @Test public void testSupportsPartiallyNonAsciiTags() {
        String body = "<div>Check</divá>";
        Document doc = Jsoup.parse(body);
        Elements els = doc.select("div");
        assertEquals("Check", els.text());
    }

    @Test public void testFragment() {
        // make sure when parsing a body fragment, a script tag at start goes into the body
        String html =
            "<script type=\"text/javascript\">console.log('foo');</script>\n" +
                "<div id=\"somecontent\">some content</div>\n" +
                "<script type=\"text/javascript\">console.log('bar');</script>";

        Document body = Jsoup.parseBodyFragment(html);
        assertEquals("<script type=\"text/javascript\">console.log('foo');</script>\n" +
            "<div id=\"somecontent\">\n" +
            " some content\n" +
            "</div>\n" +
            "<script type=\"text/javascript\">console.log('bar');</script>", body.body().html());
    }

    @Test public void testHtmlLowerCase() {
        String html = "<!doctype HTML><DIV ID=1>One</DIV>";
        Document doc = Jsoup.parse(html);
        assertEquals("<!doctype html> <html> <head></head> <body> <div id=\"1\"> One </div> </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));

        Element div = doc.selectFirst("#1");
        div.after("<TaG>One</TaG>");
        assertEquals("<tag>One</tag>", TextUtil.stripNewlines(div.nextElementSibling().outerHtml()));
    }

    @Test public void testHtmlLowerCaseAttributesOfVoidTags() {
        String html = "<!doctype HTML><IMG ALT=One></DIV>";
        Document doc = Jsoup.parse(html);
        assertEquals("<!doctype html> <html> <head></head> <body> <img alt=\"One\"> </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));
    }

    @Test public void testHtmlLowerCaseAttributesForm() {
        String html = "<form NAME=one>";
        Document doc = Jsoup.parse(html);
        assertEquals("<form name=\"one\"></form>", StringUtil.normaliseWhitespace(doc.body().html()));
    }

    @Test public void canPreserveTagCase() {
        Parser parser = Parser.htmlParser();
        parser.settings(new ParseSettings(true, false));
        Document doc = parser.parseInput("<div id=1><SPAN ID=2>", "");
        assertEquals("<html> <head></head> <body> <div id=\"1\"> <SPAN id=\"2\"></SPAN> </div> </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));

        Element div = doc.selectFirst("#1");
        div.after("<TaG ID=one>One</TaG>");
        assertEquals("<TaG id=\"one\">One</TaG>", TextUtil.stripNewlines(div.nextElementSibling().outerHtml()));
    }

    @Test public void canPreserveAttributeCase() {
        Parser parser = Parser.htmlParser();
        parser.settings(new ParseSettings(false, true));
        Document doc = parser.parseInput("<div id=1><SPAN ID=2>", "");
        assertEquals("<html> <head></head> <body> <div id=\"1\"> <span ID=\"2\"></span> </div> </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));

        Element div = doc.selectFirst("#1");
        div.after("<TaG ID=one>One</TaG>");
        assertEquals("<tag ID=\"one\">One</tag>", TextUtil.stripNewlines(div.nextElementSibling().outerHtml()));
    }

    @Test public void canPreserveBothCase() {
        Parser parser = Parser.htmlParser();
        parser.settings(new ParseSettings(true, true));
        Document doc = parser.parseInput("<div id=1><SPAN ID=2>", "");
        assertEquals("<html> <head></head> <body> <div id=\"1\"> <SPAN ID=\"2\"></SPAN> </div> </body> </html>", StringUtil.normaliseWhitespace(doc.outerHtml()));

        Element div = doc.selectFirst("#1");
        div.after("<TaG ID=one>One</TaG>");
        assertEquals("<TaG ID=\"one\">One</TaG>", TextUtil.stripNewlines(div.nextElementSibling().outerHtml()));
    }

    @Test public void handlesControlCodeInAttributeName() {
        Document doc = Jsoup.parse("<p><a \06=foo>One</a><a/\06=bar><a foo\06=bar>Two</a></p>");
        assertEquals("<p><a>One</a><a></a><a foo=\"bar\">Two</a></p>", doc.body().html());
    }

    @Test public void caseSensitiveParseTree() {
        String html = "<r><X>A</X><y>B</y></r>";
        Parser parser = Parser.htmlParser();
        parser.settings(preserveCase);
        Document doc = parser.parseInput(html, "");
        assertEquals("<r> <X> A </X> <y> B </y> </r>", StringUtil.normaliseWhitespace(doc.body().html()));
    }

    @Test public void caseInsensitiveParseTree() {
        String html = "<r><X>A</X><y>B</y></r>";
        Parser parser = Parser.htmlParser();
        Document doc = parser.parseInput(html, "");
        assertEquals("<r> <x> A </x> <y> B </y> </r>", StringUtil.normaliseWhitespace(doc.body().html()));
    }

    @Test public void preservedCaseLinksCantNest() {
        String html = "<A>ONE <A>Two</A></A>";
        Document doc = Parser.htmlParser()
            .settings(preserveCase)
            .parseInput(html, "");
        //assertEquals("<A>ONE </A><A>Two</A>", StringUtil.normaliseWhitespace(doc.body().html()));
        assertEquals("<A>ONE </A><A>Two</A>", doc.body().html());
    }

    @Test public void normalizesDiscordantTags() {
        Document document = Jsoup.parse("<div>test</DIV><p></p>");
        assertEquals("<div>\n test\n</div>\n<p></p>", document.body().html());
    }

    @Test public void selfClosingVoidIsNotAnError() {
        String html = "<p>test<br/>test<br/></p>";
        Parser parser = Parser.htmlParser().setTrackErrors(5);
        parser.parseInput(html, "");
        assertEquals(0, parser.getErrors().size());

        assertTrue(Jsoup.isValid(html, Safelist.basic()));
        String clean = Jsoup.clean(html, Safelist.basic());
        assertEquals("<p>test<br>\n test<br></p>", clean);
    }

    @Test public void selfClosingOnNonvoidIsError() {
        String html = "<p>test</p>\n\n<div /><div>Two</div>";
        Parser parser = Parser.htmlParser().setTrackErrors(5);
        parser.parseInput(html, "");
        assertEquals(1, parser.getErrors().size());
        assertEquals("<3:8>: Tag [div] cannot be self closing; not a void tag", parser.getErrors().get(0).toString());

        assertFalse(Jsoup.isValid(html, Safelist.relaxed()));
        String clean = Jsoup.clean(html, Safelist.relaxed());
        assertEquals("<p>test</p> <div></div> <div> Two </div>", StringUtil.normaliseWhitespace(clean));
    }

    @Test public void testTemplateInsideTable() throws IOException {
        File in = ParseTest.getFile("/htmltests/table-polymer-template.html");
        Document doc = Jsoup.parse(in, "UTF-8");
        doc.outputSettings().prettyPrint(true);

        Elements templates = doc.body().getElementsByTag("template");
        for (Element template : templates) {
            assertTrue(template.childNodes().size() > 1);
        }
    }

    @Test public void testHandlesDeepSpans() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("<span>");
        }

        sb.append("<p>One</p>");

        Document doc = Jsoup.parse(sb.toString());
        assertEquals(200, doc.select("span").size());
        assertEquals(1, doc.select("p").size());
    }

    @Test public void commentAtEnd() {
        Document doc = Jsoup.parse("<!");
        assertTrue(doc.childNode(0) instanceof Comment);
    }

    @Test public void preSkipsFirstNewline() {
        Document doc = Jsoup.parse("<pre>\n\nOne\nTwo\n</pre>");
        Element pre = doc.selectFirst("pre");
        assertEquals("One\nTwo", pre.text());
        assertEquals("\nOne\nTwo\n", pre.wholeText());
    }

    @Test public void handlesXmlDeclAndCommentsBeforeDoctype() throws IOException {
        File in = ParseTest.getFile("/htmltests/comments.html");
        Document doc = Jsoup.parse(in, "UTF-8");

        assertEquals("<!--?xml version=\"1.0\" encoding=\"utf-8\"?--><!-- so --> <!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><!-- what --> <html xml:lang=\"en\" lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"><!-- now --> <head><!-- then --> <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\"> <title>A Certain Kind of Test</title> </head> <body> <h1>Hello</h1>h1&gt; (There is a UTF8 hidden BOM at the top of this file.) </body> </html>",
            StringUtil.normaliseWhitespace(doc.html()));

        assertEquals("A Certain Kind of Test", doc.head().select("title").text());
    }

    @Test public void fallbackToUtfIfCantEncode() throws IOException {
        // that charset can't be encoded, so make sure we flip to utf

        String in = "<html><meta charset=\"ISO-2022-CN\"/>One</html>";
        Document doc = Jsoup.parse(new ByteArrayInputStream(in.getBytes()), null, "");

        assertEquals("UTF-8", doc.charset().name());
        assertEquals("One", doc.text());

        String html = doc.outerHtml();
        assertEquals("<html><head><meta charset=\"UTF-8\"></head><body>One</body></html>", TextUtil.stripNewlines(html));
    }

    @Test public void characterReaderBuffer() throws IOException {
        File in = ParseTest.getFile("/htmltests/character-reader-buffer.html.gz");
        Document doc = Jsoup.parse(in, "UTF-8");

        String expectedHref = "http://www.domain.com/path?param_one=value&param_two=value";

        Elements links = doc.select("a");
        assertEquals(2, links.size());
        assertEquals(expectedHref, links.get(0).attr("href")); // passes
        assertEquals(expectedHref, links.get(1).attr("href")); // fails, "but was:<...ath?param_one=value&[]_two-value>"
    }

    @Test
    public void selfClosingTextAreaDoesntLeaveDroppings() {
        // https://github.com/jhy/jsoup/issues/1220
        Document doc = Jsoup.parse("<div><div><textarea/></div></div>");
        assertFalse(doc.body().html().contains("&lt;"));
        assertFalse(doc.body().html().contains("&gt;"));
        assertEquals("<div><div><textarea></textarea></div></div>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testNoSpuriousSpace() {
        Document doc = Jsoup.parse("Just<a>One</a><a>Two</a>");
        assertEquals("Just<a>One</a><a>Two</a>", doc.body().html());
        assertEquals("JustOneTwo", doc.body().text());
    }

    @Test
    public void pTagsGetIndented() {
        String html = "<div><p><a href=one>One</a><p><a href=two>Two</a></p></div>";
        Document doc = Jsoup.parse(html);
        assertEquals("<div>\n" +
            " <p><a href=\"one\">One</a></p>\n" +
            " <p><a href=\"two\">Two</a></p>\n" +
            "</div>", doc.body().html());
    }

    @Test
    public void indentRegardlessOfCase() {
        String html = "<p>1</p><P>2</P>";
        Document doc = Jsoup.parse(html);
        assertEquals(
            "<body>\n" +
            " <p>1</p>\n" +
            " <p>2</p>\n" +
            "</body>", doc.body().outerHtml());

        Document caseDoc = Jsoup.parse(html, "", Parser.htmlParser().settings(preserveCase));
        assertEquals(
            "<body>\n" +
            " <p>1</p>\n" +
            " <P>2</P>\n" +
            "</body>", caseDoc.body().outerHtml());
    }

    @Test
    public void testH20() {
        // https://github.com/jhy/jsoup/issues/731
        String html = "H<sub>2</sub>O";
        String clean = Jsoup.clean(html, Safelist.basic());
        assertEquals("H<sub>2</sub>O", clean);

        Document doc = Jsoup.parse(html);
        assertEquals("H2O", doc.text());
    }

    @Test
    public void testUNewlines() {
        // https://github.com/jhy/jsoup/issues/851
        String html = "t<u>es</u>t <b>on</b> <i>f</i><u>ir</u>e";
        String clean = Jsoup.clean(html, Safelist.basic());
        assertEquals("t<u>es</u>t <b>on</b> <i>f</i><u>ir</u>e", clean);

        Document doc = Jsoup.parse(html);
        assertEquals("test on fire", doc.text());
    }

    @Test public void testFarsi() {
        // https://github.com/jhy/jsoup/issues/1227
        String text = "نیمه\u200Cشب";
        Document doc = Jsoup.parse("<p>" + text);
        assertEquals(text, doc.text());
    }

    @Test public void testStartOptGroup() {
        // https://github.com/jhy/jsoup/issues/1313
        String html = "<select>\n" +
            "  <optgroup label=\"a\">\n" +
            "  <option>one\n" +
            "  <option>two\n" +
            "  <option>three\n" +
            "  <optgroup label=\"b\">\n" +
            "  <option>four\n" +
            "  <option>fix\n" +
            "  <option>six\n" +
            "</select>";
        Document doc = Jsoup.parse(html);
        Element select = doc.selectFirst("select");
        assertEquals(2, select.childrenSize());

        assertEquals("<optgroup label=\"a\"> <option>one </option><option>two </option><option>three </option></optgroup><optgroup label=\"b\"> <option>four </option><option>fix </option><option>six </option></optgroup>", select.html());
    }

    @Test public void readerClosedAfterParse() {
        Document doc = Jsoup.parse("Hello");
        TreeBuilder treeBuilder = doc.parser().getTreeBuilder();
        assertNull(treeBuilder.reader);
        assertNull(treeBuilder.tokeniser);
    }

    @Test public void scriptInDataNode() {
        Document doc = Jsoup.parse("<script>Hello</script><style>There</style>");
        assertTrue(doc.selectFirst("script").childNode(0) instanceof DataNode);
        assertTrue(doc.selectFirst("style").childNode(0) instanceof DataNode);

        doc = Jsoup.parse("<SCRIPT>Hello</SCRIPT><STYLE>There</STYLE>", "", Parser.htmlParser().settings(preserveCase));
        assertTrue(doc.selectFirst("script").childNode(0) instanceof DataNode);
        assertTrue(doc.selectFirst("style").childNode(0) instanceof DataNode);
    }

    @Test public void textareaValue() {
        String html = "<TEXTAREA>YES YES</TEXTAREA>";
        Document doc = Jsoup.parse(html);
        assertEquals("YES YES", doc.selectFirst("textarea").val());

        doc = Jsoup.parse(html, "", Parser.htmlParser().settings(preserveCase));
        assertEquals("YES YES", doc.selectFirst("textarea").val());
    }

    @Test public void preserveWhitespaceInHead() {
        String html = "\n<!doctype html>\n<html>\n<head>\n<title>Hello</title>\n</head>\n<body>\n<p>One</p>\n</body>\n</html>\n";
        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        assertEquals("<!doctype html>\n<html>\n<head>\n<title>Hello</title>\n</head>\n<body>\n<p>One</p>\n</body>\n</html>\n", doc.outerHtml());
    }

    @Test public void handleContentAfterBody() {
        String html = "<body>One</body>  <p>Hello!</p></html> <p>There</p>";
        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        assertEquals("<html><head></head><body>One<p>Hello!</p><p>There</p></body>  </html> ", doc.outerHtml());
    }

    @Test public void preservesTabs() {
        // testcase to demonstrate tab retention - https://github.com/jhy/jsoup/issues/1240
        String html = "<pre>One\tTwo</pre><span>\tThree\tFour</span>";
        Document doc = Jsoup.parse(html);

        Element pre = doc.selectFirst("pre");
        Element span = doc.selectFirst("span");

        assertEquals("One\tTwo", pre.text());
        assertEquals("Three Four", span.text()); // normalized, including overall trim
        assertEquals("\tThree\tFour", span.wholeText()); // text normalizes, wholeText retains original spaces incl tabs
        assertEquals("One\tTwo Three Four", doc.body().text());

        assertEquals("<pre>One\tTwo</pre><span> Three Four</span>", doc.body().html()); // html output provides normalized space, incl tab in pre but not in span

        doc.outputSettings().prettyPrint(false);
        assertEquals(html, doc.body().html()); // disabling pretty-printing - round-trips the tab throughout, as no normalization occurs
    }

    @Test void wholeTextTreatsBRasNewline() {
        String html = "<div>\nOne<br>Two <p>Three<br>Four</div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.selectFirst("div");
        assertNotNull(div);
        assertEquals("\nOne\nTwo Three\nFour", div.wholeText());
        assertEquals("\nOne\nTwo ", div.wholeOwnText());
    }

    @Test public void canDetectAutomaticallyAddedElements() {
        String bare = "<script>One</script>";
        String full = "<html><head><title>Check</title></head><body><p>One</p></body></html>";

        assertTrue(didAddElements(bare));
        assertFalse(didAddElements(full));
    }

    private boolean didAddElements(String input) {
        // two passes, one as XML and one as HTML. XML does not vivify missing/optional tags
        Document html = Jsoup.parse(input);
        Document xml = Jsoup.parse(input, "", Parser.xmlParser());

        int htmlElementCount = html.getAllElements().size();
        int xmlElementCount = xml.getAllElements().size();
        return htmlElementCount > xmlElementCount;
    }

    @Test public void canSetHtmlOnCreatedTableElements() {
        // https://github.com/jhy/jsoup/issues/1603
        Element element = new Element("tr");
        element.html("<tr><td>One</td></tr>");
        assertEquals("<tr>\n <tr>\n  <td>One</td>\n </tr>\n</tr>", element.outerHtml());
    }

    @Test public void parseFragmentOnCreatedDocument() {
        // https://github.com/jhy/jsoup/issues/1601
        String bareFragment = "<h2>text</h2>";
        List<Node> nodes = new Document("").parser().parseFragmentInput(bareFragment, new Element("p"), "");
        assertEquals(1, nodes.size());
        Node node = nodes.get(0);
        assertEquals("h2", node.nodeName());
        assertEquals("<p><h2>text</h2></p>", node.parent().outerHtml());
    }

    @Test public void nestedPFragments() {
        // https://github.com/jhy/jsoup/issues/1602
        String bareFragment = "<p></p><a></a>";
        List<Node> nodes = new Document("").parser().parseFragmentInput(bareFragment, new Element("p"), "");
        assertEquals(2, nodes.size());
        Node node = nodes.get(0);
        assertEquals("<p><p></p><a></a></p>", node.parent().outerHtml()); // mis-nested because fragment forced into the element, OK
    }

    @Test public void nestedAnchorAdoption() {
        // https://github.com/jhy/jsoup/issues/1608
        String html = "<a>\n<b>\n<div>\n<a>test</a>\n</div>\n</b>\n</a>";
        Document doc = Jsoup.parse(html);
        assertNotNull(doc);
        assertEquals("<a> <b> </b></a><b><div><a> </a><a>test</a></div></b>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void tagsMustStartWithAscii() {
        // https://github.com/jhy/jsoup/issues/1006
        String[] valid = {"a一", "a会员挂单金额5", "table(╯°□°)╯"};
        String[] invalid = {"一", "会员挂单金额5", "(╯°□°)╯"};

        for (String tag : valid) {
            Document doc = Jsoup.parse("<" + tag + ">Text</" + tag + ">");
            Elements els = doc.getElementsByTag(tag);
            assertEquals(1, els.size());
            assertEquals(tag, els.get(0).tagName());
            assertEquals("Text", els.get(0).text());
        }

        for (String tag : invalid) {
            Document doc = Jsoup.parse("<" + tag + ">Text</" + tag + ">");
            Elements els = doc.getElementsByTag(tag);
            assertEquals(0, els.size());
            assertEquals("&lt;" + tag + "&gt;Text<!--/" + tag + "-->", doc.body().html());
        }
    }

    @Test void htmlOutputCorrectsInvalidAttributeNames() {
        String html = "<body style=\"color: red\" \" name\"><div =\"\"></div></body>";
        Document doc = Jsoup.parse(html);
        assertEquals(Document.OutputSettings.Syntax.html, doc.outputSettings().syntax());

        String out = doc.body().outerHtml();
        assertEquals("<body style=\"color: red\" name>\n <div></div>\n</body>", out);
    }

    @Test void templateInHead() {
        // https://try.jsoup.org/~EGp3UZxQe503TJDHQEQEzm8IeUc
        String html = "<head><template id=1><meta name=tmpl></template><title>Test</title><style>One</style></head><body><p>Two</p>";
        Document doc = Jsoup.parse(html);

        String want = "<html><head><template id=\"1\"><meta name=\"tmpl\"></template><title>Test</title><style>One</style></head><body><p>Two</p></body></html>";
        assertEquals(want, TextUtil.stripNewlines(doc.html()));

        Elements template = doc.select("template#1");
        template.select("meta").attr("content", "Yes");
        template.unwrap();

        want = "<html><head><meta name=\"tmpl\" content=\"Yes\"><title>Test</title><style>One</style></head><body><p>Two</p></body></html>";
        assertEquals(want, TextUtil.stripNewlines(doc.html()));
    }

    @Test void nestedTemplateInBody() {
        String html = "<body><template id=1><table><tr><template id=2><td>One</td><td>Two</td></template></tr></template></body>";
        Document doc = Jsoup.parse(html);

        String want = "<html><head></head><body><template id=\"1\"><table><tbody><tr><template id=\"2\"><td>One</td><td>Two</td></template></tr></tbody></table></template></body></html>";
        assertEquals(want, TextUtil.stripNewlines(doc.html()));

        // todo - will be nice to add some simpler template element handling like clone children etc?
        Element tmplTbl = doc.selectFirst("template#1");
        Element tmplRow = doc.selectFirst("template#2");
        assertNotNull(tmplRow);
        assertNotNull(tmplTbl);
        tmplRow.appendChild(tmplRow.clone());
        doc.select("template").unwrap();

        want = "<html><head></head><body><table><tbody><tr><td>One</td><td>Two</td><td>One</td><td>Two</td></tr></tbody></table></body></html>";
        assertEquals(want, TextUtil.stripNewlines(doc.html()));
    }

    @Test void canSelectIntoTemplate() {
        String html = "<body><div><template><p>Hello</p>";
        Document doc = Jsoup.parse(html);
        String want = "<html><head></head><body><div><template><p>Hello</p></template></div></body></html>";
        assertEquals(want, TextUtil.stripNewlines(doc.html()));

        Element p = doc.selectFirst("div p");
        Element p1 = doc.selectFirst("template :containsOwn(Hello)");
        assertEquals("p", p.normalName());
        assertEquals(p, p1);
    }

    @Test void tableRowFragment() {
        Document doc = Jsoup.parse("<body><table></table></body");
        String html = "<tr><td><img></td></tr>";
        Element table = doc.selectFirst("table");
        table.html(html); // invokes the fragment parser with table as context
        String want = "<tbody><tr><td><img></td></tr></tbody>";
        assertEquals(want, TextUtil.stripNewlines(table.html()));
        want = "<table><tbody><tr><td><img></td></tr></tbody></table>";
        assertEquals(want, TextUtil.stripNewlines(doc.body().html()));
    }

    @Test void templateTableRowFragment() {
        // https://github.com/jhy/jsoup/issues/1409 (per the fragment <tr> use case)
        Document doc = Jsoup.parse("<body><table><template></template></table></body");
        String html = "<tr><td><img></td></tr>";
        Element tmpl = doc.selectFirst("template");
        tmpl.html(html); // invokes the fragment parser with template as context
        String want = "<tr><td><img></td></tr>";
        assertEquals(want, TextUtil.stripNewlines(tmpl.html()));
        tmpl.unwrap();

        want = "<html><head></head><body><table><tr><td><img></td></tr></table></body></html>";
        assertEquals(want, TextUtil.stripNewlines(doc.html()));
    }

    @Test void templateNotInTableRowFragment() {
        // https://github.com/jhy/jsoup/issues/1409 (per the fragment <tr> use case)
        Document doc = Jsoup.parse("<body><template></template></body");
        String html = "<tr><td><img></td></tr>";
        Element tmpl = doc.selectFirst("template");
        tmpl.html(html); // invokes the fragment parser with template as context
        String want = "<tr><td><img></td></tr>";
        assertEquals(want, TextUtil.stripNewlines(tmpl.html()));
        tmpl.unwrap();

        want = "<html><head></head><body><tr><td><img></td></tr></body></html>";
        assertEquals(want, TextUtil.stripNewlines(doc.html()));
    }

    @Test void templateFragment() {
        // https://github.com/jhy/jsoup/issues/1315
        String html = "<template id=\"lorem-ipsum\"><tr><td>Lorem</td><td>Ipsum</td></tr></template>";
        Document frag = Jsoup.parseBodyFragment(html);
        String want = "<template id=\"lorem-ipsum\"><tr><td>Lorem</td><td>Ipsum</td></tr></template>";
        assertEquals(want, TextUtil.stripNewlines(frag.body().html()));
    }

    @Test void templateInferredForm() {
        // https://github.com/jhy/jsoup/issues/1637 | https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=38987
        Document doc = Jsoup.parse("<template><isindex action>");
        assertNotNull(doc);
        assertEquals("<template><form><hr><label>This is a searchable index. Enter search keywords: <input name=\"isindex\"></label><hr></form></template>",
            TextUtil.stripNewlines(doc.head().html()));
    }

    @Test void trimNormalizeElementNamesInBuilder() {
        // https://github.com/jhy/jsoup/issues/1637 | https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=38983
        // This is interesting - in TB state, the element name was "template\u001E", so no name checks matched. Then,
        // when the Element is created, the name got normalized to "template" and so looked like there should be a
        // template on the stack during resetInsertionMode for the select.
        // The issue was that the normalization in Tag.valueOf did a trim which the Token.Tag did not
        Document doc = Jsoup.parse("<template\u001E<select<input<");
        assertNotNull(doc);
        assertEquals("<template><select></select><input>&lt;</template>",
            TextUtil.stripNewlines(doc.head().html()));
    }

    @Test void errorsBeforeHtml() {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(10);
        Document doc = Jsoup.parse("<!doctype html><!doctype something></div>", parser);
        ParseErrorList errors = parser.getErrors();
        assertEquals(2, errors.size());
        assertEquals("<1:36>: Unexpected Doctype token [<!doctype something>] when in state [BeforeHtml]", errors.get(0).toString());
        assertEquals("<1:42>: Unexpected EndTag token [</div>] when in state [BeforeHtml]", errors.get(1).toString());
        assertEquals("<!doctype html><html><head></head><body></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test void afterHeadReAdds() {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(10);
        Document doc = Jsoup.parse("<head></head><meta charset=UTF8><p>Hello", parser);
        ParseErrorList errors = parser.getErrors();
        assertEquals(1, errors.size());
        assertEquals("<1:33>: Unexpected StartTag token [<meta  charset=\"UTF8\">] when in state [AfterHead]", errors.get(0).toString());
        assertEquals("<html><head><meta charset=\"UTF8\"></head><body><p>Hello</p></body></html>", TextUtil.stripNewlines(doc.html()));
        // meta gets added back into head
    }

    @Test void mergeHtmlAttributesFromBody() {
        Document doc = Jsoup.parse("<html id=1 class=foo><body><html class=bar data=x><p>One");
        assertEquals("<html id=\"1\" class=\"foo\" data=\"x\"><head></head><body><p>One</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test void mergeHtmlNoAttributesFromBody() {
        Document doc = Jsoup.parse("<html id=1 class=foo><body><html><p>One");
        assertEquals("<html id=\"1\" class=\"foo\"><head></head><body><p>One</p></body></html>", TextUtil.stripNewlines(doc.html()));
    }
}
