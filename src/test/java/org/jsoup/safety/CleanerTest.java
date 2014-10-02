package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 Tests for the cleaner.

 @author Jonathan Hedley, jonathan@hedley.net */
public class CleanerTest {
    @Test public void simpleBehaviourTest() {
        String h = "<div><p class=foo><a href='http://evil.com'>Hello <b id=bar>there</b>!</a></div>";
        String cleanHtml = Jsoup.clean(h, Whitelist.simpleText());

        assertEquals("Hello <b>there</b>!", TextUtil.stripNewlines(cleanHtml));
    }
    
    @Test public void simpleBehaviourTest2() {
        String h = "Hello <b>there</b>!";
        String cleanHtml = Jsoup.clean(h, Whitelist.simpleText());

        assertEquals("Hello <b>there</b>!", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void basicBehaviourTest() {
        String h = "<div><p><a href='javascript:sendAllMoney()'>Dodgy</a> <A HREF='HTTP://nice.com'>Nice</a></p><blockquote>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Whitelist.basic());

        assertEquals("<p><a rel=\"nofollow\">Dodgy</a> <a href=\"http://nice.com\" rel=\"nofollow\">Nice</a></p><blockquote>Hello</blockquote>",
                TextUtil.stripNewlines(cleanHtml));
    }
    
    @Test public void basicWithImagesTest() {
        String h = "<div><p><img src='http://example.com/' alt=Image></p><p><img src='ftp://ftp.example.com'></p></div>";
        String cleanHtml = Jsoup.clean(h, Whitelist.basicWithImages());
        assertEquals("<p><img src=\"http://example.com/\" alt=\"Image\"></p><p><img></p>", TextUtil.stripNewlines(cleanHtml));
    }
    
    @Test public void testRelaxed() {
        String h = "<h1>Head</h1><table><tr><td>One<td>Two</td></tr></table>";
        String cleanHtml = Jsoup.clean(h, Whitelist.relaxed());
        assertEquals("<h1>Head</h1><table><tbody><tr><td>One</td><td>Two</td></tr></tbody></table>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRemoveTags() {
        String h = "<div><p><A HREF='HTTP://nice.com'>Nice</a></p><blockquote>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Whitelist.basic().removeTags("a"));

        assertEquals("<p>Nice</p><blockquote>Hello</blockquote>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRemoveAttributes() {
        String h = "<div><p>Nice</p><blockquote cite='http://example.com/quotations'>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Whitelist.basic().removeAttributes("blockquote", "cite"));

        assertEquals("<p>Nice</p><blockquote>Hello</blockquote>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRemoveEnforcedAttributes() {
        String h = "<div><p><A HREF='HTTP://nice.com'>Nice</a></p><blockquote>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Whitelist.basic().removeEnforcedAttribute("a", "rel"));

        assertEquals("<p><a href=\"http://nice.com\">Nice</a></p><blockquote>Hello</blockquote>",
                TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRemoveProtocols() {
        String h = "<p>Contact me <a href='mailto:info@example.com'>here</a></p>";
        String cleanHtml = Jsoup.clean(h, Whitelist.basic().removeProtocols("a", "href", "ftp", "mailto"));

        assertEquals("<p>Contact me <a rel=\"nofollow\">here</a></p>",
                TextUtil.stripNewlines(cleanHtml));
    }
    
    @Test public void testDropComments() {
        String h = "<p>Hello<!-- no --></p>";
        String cleanHtml = Jsoup.clean(h, Whitelist.relaxed());
        assertEquals("<p>Hello</p>", cleanHtml);
    }
    
    @Test public void testDropXmlProc() {
        String h = "<?import namespace=\"xss\"><p>Hello</p>";
        String cleanHtml = Jsoup.clean(h, Whitelist.relaxed());
        assertEquals("<p>Hello</p>", cleanHtml);
    }
    
    @Test public void testDropScript() {
        String h = "<SCRIPT SRC=//ha.ckers.org/.j><SCRIPT>alert(/XSS/.source)</SCRIPT>";
        String cleanHtml = Jsoup.clean(h, Whitelist.relaxed());
        assertEquals("", cleanHtml);
    }
    
    @Test public void testDropImageScript() {
        String h = "<IMG SRC=\"javascript:alert('XSS')\">";
        String cleanHtml = Jsoup.clean(h, Whitelist.relaxed());
        assertEquals("<img>", cleanHtml);
    }
    
    @Test public void testCleanJavascriptHref() {
        String h = "<A HREF=\"javascript:document.location='http://www.google.com/'\">XSS</A>";
        String cleanHtml = Jsoup.clean(h, Whitelist.relaxed());
        assertEquals("<a>XSS</a>", cleanHtml);
    }

    @Test public void testCleanAnchorProtocol() {
        String validAnchor = "<a href=\"#valid\">Valid anchor</a>";
        String invalidAnchor = "<a href=\"#anchor with spaces\">Invalid anchor</a>";

        // A Whitelist that does not allow anchors will strip them out.
        String cleanHtml = Jsoup.clean(validAnchor, Whitelist.relaxed());
        assertEquals("<a>Valid anchor</a>", cleanHtml);

        cleanHtml = Jsoup.clean(invalidAnchor, Whitelist.relaxed());
        assertEquals("<a>Invalid anchor</a>", cleanHtml);

        // A Whitelist that allows them will keep them.
        Whitelist relaxedWithAnchor = Whitelist.relaxed().addProtocols("a", "href", "#");

        cleanHtml = Jsoup.clean(validAnchor, relaxedWithAnchor);
        assertEquals(validAnchor, cleanHtml);

        // An invalid anchor is never valid.
        cleanHtml = Jsoup.clean(invalidAnchor, relaxedWithAnchor);
        assertEquals("<a>Invalid anchor</a>", cleanHtml);
    }

    @Test public void testDropsUnknownTags() {
        String h = "<p><custom foo=true>Test</custom></p>";
        String cleanHtml = Jsoup.clean(h, Whitelist.relaxed());
        assertEquals("<p>Test</p>", cleanHtml);
    }
    
    @Test public void testHandlesEmptyAttributes() {
        String h = "<img alt=\"\" src= unknown=''>";
        String cleanHtml = Jsoup.clean(h, Whitelist.basicWithImages());
        assertEquals("<img alt=\"\">", cleanHtml);
    }

    @Test public void testIsValid() {
        String ok = "<p>Test <b><a href='http://example.com/'>OK</a></b></p>";
        String nok1 = "<p><script></script>Not <b>OK</b></p>";
        String nok2 = "<p align=right>Test Not <b>OK</b></p>";
        String nok3 = "<!-- comment --><p>Not OK</p>"; // comments and the like will be cleaned
        assertTrue(Jsoup.isValid(ok, Whitelist.basic()));
        assertFalse(Jsoup.isValid(nok1, Whitelist.basic()));
        assertFalse(Jsoup.isValid(nok2, Whitelist.basic()));
        assertFalse(Jsoup.isValid(nok3, Whitelist.basic()));
    }
    
    @Test public void resolvesRelativeLinks() {
        String html = "<a href='/foo'>Link</a><img src='/bar'>";
        String clean = Jsoup.clean(html, "http://example.com/", Whitelist.basicWithImages());
        assertEquals("<a href=\"http://example.com/foo\" rel=\"nofollow\">Link</a>\n<img src=\"http://example.com/bar\">", clean);
    }

    @Test public void preservesRelativeLinksIfConfigured() {
        String html = "<a href='/foo'>Link</a><img src='/bar'> <img src='javascript:alert()'>";
        String clean = Jsoup.clean(html, "http://example.com/", Whitelist.basicWithImages().preserveRelativeLinks(true));
        assertEquals("<a href=\"/foo\" rel=\"nofollow\">Link</a>\n<img src=\"/bar\"> \n<img>", clean);
    }
    
    @Test public void dropsUnresolvableRelativeLinks() {
        String html = "<a href='/foo'>Link</a>";
        String clean = Jsoup.clean(html, Whitelist.basic());
        assertEquals("<a rel=\"nofollow\">Link</a>", clean);
    }

    @Test public void handlesCustomProtocols() {
        String html = "<img src='cid:12345' /> <img src='data:gzzt' />";
        String dropped = Jsoup.clean(html, Whitelist.basicWithImages());
        assertEquals("<img> \n<img>", dropped);

        String preserved = Jsoup.clean(html, Whitelist.basicWithImages().addProtocols("img", "src", "cid", "data"));
        assertEquals("<img src=\"cid:12345\"> \n<img src=\"data:gzzt\">", preserved);
    }

    @Test public void handlesAllPseudoTag() {
        String html = "<p class='foo' src='bar'><a class='qux'>link</a></p>";
        Whitelist whitelist = new Whitelist()
                .addAttributes(":all", "class")
                .addAttributes("p", "style")
                .addTags("p", "a");

        String clean = Jsoup.clean(html, whitelist);
        assertEquals("<p class=\"foo\"><a class=\"qux\">link</a></p>", clean);
    }

    @Test public void addsTagOnAttributesIfNotSet() {
        String html = "<p class='foo' src='bar'>One</p>";
        Whitelist whitelist = new Whitelist()
            .addAttributes("p", "class");
        // ^^ whitelist does not have explicit tag add for p, inferred from add attributes.
        String clean = Jsoup.clean(html, whitelist);
        assertEquals("<p class=\"foo\">One</p>", clean);
    }

    @Test public void supplyOutputSettings() {
        // test that one can override the default document output settings
        Document.OutputSettings os = new Document.OutputSettings();
        os.prettyPrint(false);
        os.escapeMode(Entities.EscapeMode.extended);
        os.charset("ascii");

        String html = "<div><p>&bernou;</p></div>";
        String customOut = Jsoup.clean(html, "http://foo.com/", Whitelist.relaxed(), os);
        String defaultOut = Jsoup.clean(html, "http://foo.com/", Whitelist.relaxed());
        assertNotSame(defaultOut, customOut);

        assertEquals("<div><p>&bernou;</p></div>", customOut);
        assertEquals("<div>\n" +
            " <p>ℬ</p>\n" +
            "</div>", defaultOut);

        os.charset("ASCII");
        os.escapeMode(Entities.EscapeMode.base);
        String customOut2 = Jsoup.clean(html, "http://foo.com/", Whitelist.relaxed(), os);
        assertEquals("<div><p>&#x212c;</p></div>", customOut2);
    }

    @Test public void handlesFramesets() {
        String dirty = "<html><head><script></script><noscript></noscript></head><frameset><frame src=\"foo\" /><frame src=\"foo\" /></frameset></html>";
        String clean = Jsoup.clean(dirty, Whitelist.basic());
        assertEquals("", clean); // nothing good can come out of that

        Document dirtyDoc = Jsoup.parse(dirty);
        Document cleanDoc = new Cleaner(Whitelist.basic()).clean(dirtyDoc);
        assertFalse(cleanDoc == null);
        assertEquals(0, cleanDoc.body().childNodeSize());
    }

    @Test public void cleansInternationalText() {
        assertEquals("привет", Jsoup.clean("привет", Whitelist.none()));
    }

    @Test
    public void testScriptTagInWhiteList() {
        Whitelist whitelist = Whitelist.relaxed();
        whitelist.addTags( "script" );
        assertTrue( Jsoup.isValid("Hello<script>alert('Doh')</script>World !", whitelist ) );
    }
}
