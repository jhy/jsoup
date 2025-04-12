package org.jsoup.parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.jsoup.parser.Parser.NamespaceHtml;
import static org.junit.jupiter.api.Assertions.*;

public class TagSetTest {
    @Test void canRetrieveNewTagsSensitive() {
        Document doc = Jsoup.parse("<div><p>One</p></div>", "", Parser.htmlParser().settings(ParseSettings.preserveCase));
        TagSet tags = doc.parser().tagSet();
        // should be the full html set
        Tag meta = tags.get("meta", NamespaceHtml);
        assertNotNull(meta);
        assertTrue(meta.isKnownTag());

        Element p = doc.expectFirst("p");
        assertTrue(p.tag().isKnownTag());

        assertNull(tags.get("FOO", NamespaceHtml));
        p.tagName("FOO");
        Tag foo = p.tag();
        assertEquals("FOO", foo.name());
        assertEquals("foo", foo.normalName());
        assertEquals(NamespaceHtml, foo.namespace());
        assertFalse(foo.isKnownTag());

        assertSame(foo, tags.get("FOO", NamespaceHtml));
        assertSame(foo, tags.valueOf("FOO", NamespaceHtml));
        assertNull(tags.get("FOO", "SomeOtherNamespace"));
    }

    @Test void canRetrieveNewTagsInsensitive() {
        Document doc = Jsoup.parse("<div><p>One</p></div>");
        TagSet tags = doc.parser().tagSet();
        // should be the full html set
        Tag meta = tags.get("meta", NamespaceHtml);
        assertNotNull(meta);
        assertTrue(meta.isKnownTag());

        Element p = doc.expectFirst("p");
        assertTrue(p.tag().isKnownTag());

        assertNull(tags.get("FOO", NamespaceHtml));
        p.tagName("FOO");
        Tag foo = p.tag();
        assertEquals("foo", foo.name());
        assertEquals("foo", foo.normalName());
        assertEquals(NamespaceHtml, foo.namespace());
        assertFalse(foo.isKnownTag());

        assertSame(foo, tags.get("foo", NamespaceHtml));
        assertSame(foo, tags.valueOf("FOO", NamespaceHtml, doc.parser().settings()));
        assertNull(tags.get("foo", "SomeOtherNamespace"));
    }

    @Test void supplyCustomTagSet() {
        TagSet tags = TagSet.Html();
        tags.valueOf("custom", NamespaceHtml).set(Tag.PreserveWhitespace).set(Tag.Block);
        Parser parser = Parser.htmlParser().tagSet(tags);

        Document doc = Jsoup.parse("<body><custom>\n\nFoo\n Bar</custom></body>", parser);
        Element custom = doc.expectFirst("custom");
        assertTrue(custom.tag().preserveWhitespace());
        assertTrue(custom.tag().isBlock());
        assertEquals("<custom>\n" +
            "\n" +
            "Foo\n" +
            " Bar" +
            "</custom>", custom.outerHtml());
    }

    @Test void knownTags() {
        // tests that tags explicitly inserted via .add are 'known'; those that come implicitly via valueOf are not
        TagSet tags = TagSet.Html();
        Tag custom = new Tag("custom");
        assertEquals("custom", custom.name());
        assertEquals(NamespaceHtml, custom.namespace());
        assertFalse(custom.isKnownTag()); // not yet

        Tag br = tags.get("br", NamespaceHtml);
        assertNotNull(br);
        assertTrue(br.isKnownTag());
        assertSame(br, tags.valueOf("br", NamespaceHtml));

        Tag foo = tags.valueOf("foo", NamespaceHtml);
        assertFalse(foo.isKnownTag());

        tags.add(custom);
        assertTrue(custom.isKnownTag());
        assertSame(custom, tags.get("custom", NamespaceHtml));
        assertSame(custom, tags.valueOf("custom", NamespaceHtml));
        Tag capCustom = tags.valueOf("Custom", NamespaceHtml);
        assertTrue(capCustom.isKnownTag()); // cloned from a known tag, so is still known

        // known if set or clear called
        Tag c1 = new Tag("bar");
        assertFalse(c1.isKnownTag());
        c1.set(Tag.Block);
        assertTrue(c1.isKnownTag());
        c1.clear(Tag.Block);
        assertTrue(c1.isKnownTag());
        c1.clear(Tag.Known);
        assertFalse(c1.isKnownTag());
    }
}
