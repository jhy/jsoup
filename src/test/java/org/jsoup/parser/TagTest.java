package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.MultiLocaleExtension.MultiLocaleTest;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import static org.jsoup.parser.Parser.NamespaceHtml;
import static org.jsoup.parser.Parser.NamespaceSvg;
import static org.junit.jupiter.api.Assertions.*;

/**
 Tag tests.
 @author Jonathan Hedley, jonathan@hedley.net */
public class TagTest {
    @Test public void isCaseSensitive() {
        Tag p1 = Tag.valueOf("P");
        Tag p2 = Tag.valueOf("p");
        assertNotEquals(p1, p2);
    }

    @MultiLocaleTest
    public void canBeInsensitive(Locale locale) {
        Locale.setDefault(locale);

        Tag script1 = Tag.valueOf("script", NamespaceHtml, ParseSettings.htmlDefault);
        Tag script2 = Tag.valueOf("SCRIPT", NamespaceHtml, ParseSettings.htmlDefault);
        assertEquals(script1, script2);

        TagSet htmlTags = TagSet.Html();
        Tag script3 = htmlTags.valueOf("script", NamespaceHtml, ParseSettings.htmlDefault);
        Tag script4 = htmlTags.valueOf("SCRIPT", NamespaceHtml, ParseSettings.htmlDefault);
        assertSame(script3, script4);
    }

    @Test public void trims() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf(" p ");
        assertEquals(p1, p2);
    }

    @Test public void equality() {
        Tag p1 = Tag.valueOf("p");
        Tag p2 = Tag.valueOf("p");
        assertEquals(p1, p2);
        assertNotSame(p1, p2); // not same because Tag.valueOf creates new clone of the TagSet.Html, so changes don't clobber all

        TagSet html1 = TagSet.Html();
        TagSet html2 = TagSet.Html();
        assertEquals(html1, html2);
        assertNotSame(html1, html2);

        Tag p3 = html1.valueOf("p", NamespaceHtml);
        Tag p4 = html1.valueOf("p", NamespaceHtml);
        Tag p5 = html2.valueOf("p", NamespaceHtml);
        Tag p6 = html2.valueOf("p", NamespaceHtml);
        assertEquals(p1, p3);
        assertEquals(p3, p4);
        assertEquals(p4, p5);
        assertSame(p3, p4);
        assertSame(p5, p6);
        assertNotSame(p3, p5);
    }

    @Test public void divSemantics() {
        Tag div = Tag.valueOf("div");

        assertTrue(div.isBlock());
        assertFalse(div.isInline());
        assertTrue(div.isKnownTag());
    }

    @Test public void pSemantics() {
        Tag p = Tag.valueOf("p");
        assertTrue(p.isKnownTag());
        assertTrue(p.isBlock());
        assertFalse(p.isInline());
    }

    @Test public void imgSemantics() {
        Tag img = Tag.valueOf("img");
        assertTrue(img.isInline());
        assertTrue(img.isSelfClosing());
        assertFalse(img.isBlock());
    }

    @Test public void defaultSemantics() {
        Tag foo = Tag.valueOf("FOO"); // not defined
        Tag foo2 = Tag.valueOf("FOO");

        assertEquals(foo, foo2);
        assertFalse(foo.isKnownTag());
        assertTrue(foo.isInline());
        assertFalse(foo.isBlock());
        assertFalse(foo.is(Tag.InlineContainer));
        assertFalse(foo.preserveWhitespace());
    }

    @Test public void valueOfChecksNotNull() {
        assertThrows(IllegalArgumentException.class, () -> Tag.valueOf(null));
    }

    @Test public void valueOfChecksNotEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Tag.valueOf(" "));
    }

    @Test public void knownTags() {
        assertTrue(Tag.isKnownTag("div"));
        assertFalse(Tag.isKnownTag("explain"));
    }

    @Test public void knownSvgNamespace() {
        Tag svgHtml = Tag.valueOf("svg"); // no namespace specified, defaults to html, so not the known tag
        Tag svg = Tag.valueOf("svg", Parser.NamespaceSvg, ParseSettings.htmlDefault);

        assertEquals(NamespaceHtml, svgHtml.namespace());
        assertEquals(Parser.NamespaceSvg, svg.namespace());

        assertFalse(svgHtml.isKnownTag()); // generated
        assertTrue(svg.isKnownTag()); // known
    }

    @Test public void unknownTagNamespace() {
        Tag fooHtml = Tag.valueOf("foo"); // no namespace specified, defaults to html
        Tag foo = Tag.valueOf("foo", Parser.NamespaceSvg, ParseSettings.htmlDefault);

        assertEquals(NamespaceHtml, fooHtml.namespace());
        assertEquals(Parser.NamespaceSvg, foo.namespace());

        assertFalse(fooHtml.isKnownTag()); // generated
        assertFalse(foo.isKnownTag()); // generated
    }

    @Test void canSetOptions() {
        Tag tag = new Tag("foo", NamespaceHtml);
        assertFalse(tag.isKnownTag());
        assertFalse(tag.isEmpty());
        tag.set(Tag.Void);
        assertTrue(tag.isEmpty());
        assertTrue(tag.isKnownTag());
    }

    @Test void updateNameAndNamespace() {
        Tag tag = new Tag("foo", NamespaceHtml);
        tag.name("bar").namespace(NamespaceSvg);
        tag.set(Tag.Block);
        assertEquals("bar", tag.name());
        assertEquals(NamespaceSvg, tag.namespace());
        assertTrue(tag.isBlock()); // properties are unchanged

        // test in a doc
        Document doc = Jsoup.parse("<foo>One</foo><foo>Two</foo>");
        Tag foo = doc.expectFirst("foo").tag();
        foo.name("BAR");
        assertEquals("<BAR>One</BAR><BAR>Two</BAR>", doc.body().html()); // is case-sensitive
    }

    @Test void formSubmittable() {
        // https://github.com/jhy/jsoup/issues/2323
        Tag img = Tag.valueOf("img");
        Tag input = Tag.valueOf("input");
        int imgOpts = img.options;
        int inputOpts = input.options;
        assertFalse(img.isFormSubmittable());
        assertTrue(input.isFormSubmittable());
        assertEquals(imgOpts, img.options);
        assertEquals(inputOpts, input.options);
    }

    @Test void stableHashcode() {
        // tests that the hashcode is stable and suitable as a key
        HashSet<Tag> tags = new HashSet<>();
        Tag img = Tag.valueOf("img");
        Tag IMG = Tag.valueOf("IMG");
        Tag imgS = Tag.valueOf("img", NamespaceSvg, ParseSettings.htmlDefault);

        assertEquals(-2074969810, img.hashCode());
        assertEquals(-2075954866, IMG.hashCode());
        assertEquals(-292873947, imgS.hashCode());

        tags.add(img);
        tags.add(IMG);
        tags.add(imgS);

        imgS.set(Tag.Block);
        assertEquals(-292873947, imgS.hashCode());

        assertTrue(tags.contains(img));
        assertTrue(tags.contains(IMG));
        assertTrue(tags.contains(imgS));
    }

    @Test void prefix() {
        Tag img = Tag.valueOf("img");
        Tag book = Tag.valueOf("bk:book");

        assertEquals("", img.prefix());
        assertEquals("bk", book.prefix());
    }

    @Test void localname() {
        Tag img = Tag.valueOf("img");
        Tag book = Tag.valueOf("bk:book");

        assertEquals("img", img.localName());
        assertEquals("book", book.localName());
    }

    @Test void valueOfWithSettings() {
        Tag img1 = Tag.valueOf("img", ParseSettings.htmlDefault);
        Tag img2 = Tag.valueOf("IMG", ParseSettings.htmlDefault);
        Tag img3 = Tag.valueOf("IMG", ParseSettings.preserveCase);

        assertNotSame(img1, img2); // because we are creating new TagSets with html()
        assertNotSame(img1, img3);
        assertEquals("IMG", img3.toString());
        assertEquals("img", img1.toString());

        TagSet tagSet = TagSet.Html();
        assertSame(
            tagSet.valueOf("img", NamespaceHtml, ParseSettings.htmlDefault),
            tagSet.valueOf("IMG", NamespaceHtml, ParseSettings.htmlDefault)
        );

        assertNotSame(
            tagSet.valueOf("img", NamespaceHtml),
            tagSet.valueOf("IMG", NamespaceHtml)
        );
    }

    @Test void equals() {
        TagSet tags = TagSet.Html();
        Tag p1 = tags.get("p", NamespaceHtml);
        Tag p2 = p1.clone();
        assertEquals(p1, p2);
        assertNotEquals(p1, tags);

        p2.namespace = "Other";
        assertNotEquals(p1, p2);
        p2.namespace = p1.namespace;

        p2.tagName = "P";
        assertNotEquals(p1, p2);
        p2.tagName = p1.tagName;

        p2.normalName = "pp";
        assertNotEquals(p1, p2);
        p2.normalName = p1.normalName;

        p2.options = 0;
        assertNotEquals(p1, p2);
        p2.options = p1.options;
    }
}
