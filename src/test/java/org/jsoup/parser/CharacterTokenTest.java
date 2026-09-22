package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class CharacterTokenTest {
    @Test void normalizesNulls() {
        Token.Character token = new Token.Character().data("a\0b");
        assertTrue(token.hasNull);
        token.normalizeNulls(false);
        assertEquals("ab", token.getData());
        assertFalse(token.hasNull);

        token.data("c\0d");
        assertTrue(token.hasNull);
        token.normalizeNulls(true);
        assertEquals("c�d", token.getData());
        assertFalse(token.hasNull);
        token.normalizeNulls(false);
        assertEquals("c�d", token.getData());

        token.data("\0").data("safe");
        assertFalse(token.hasNull);
    }

    @Test void copiesNullFlag() {
        Token.Character original = new Token.Character().data("a\0b");
        Token.Character copy = new Token.Character(original);
        original.reset();
        assertFalse(original.hasNull);
        assertTrue(copy.hasNull);
        copy.normalizeNulls(true);
        assertEquals("a�b", copy.getData());
        assertFalse(copy.hasNull);
        assertEquals("", original.getData());
    }

    @Test void cdataNulls() {
        Token.CData token = new Token.CData("a\0b");
        assertTrue(token.hasNull);
        token.normalizeNulls(true);
        assertEquals("a�b", token.getData());
        assertFalse(token.hasNull);
        assertEquals("a\0b", Jsoup.parse("<x><![CDATA[a\0b]]></x>", "", Parser.xmlParser()).expectFirst("x").wholeText());
    }

    @Test void resetsNullFlagOnReuse() {
        HtmlTreeBuilder builder = new HtmlTreeBuilder();
        Parser parser = new Parser(builder);
        builder.initialiseParse(new StringReader("a\0b<p>safe</p>\0"), "", parser);
        try {
            Token first = builder.tokeniser.read();
            assertTrue(first.asCharacter().hasNull);
            assertEquals("a\0b", first.asCharacter().getData());
            first.reset();
            builder.tokeniser.read().reset(); // start tag
            Token safe = builder.tokeniser.read();
            assertSame(first, safe);
            assertFalse(safe.asCharacter().hasNull);
            assertEquals("safe", safe.asCharacter().getData());
            safe.reset();
            builder.tokeniser.read().reset(); // end tag
            assertTrue(builder.tokeniser.read().asCharacter().hasNull);
        } finally {
            builder.reader.close();
        }
    }

    @Test void nullReferences() {
        for (TreeBuilder builder : new TreeBuilder[]{new HtmlTreeBuilder(), new XmlTreeBuilder()}) {
            builder.initialiseParse(new StringReader("&#0;"), "", new Parser(builder));
            try {
                Token.Character token = builder.tokeniser.read().asCharacter();
                boolean xml = builder instanceof XmlTreeBuilder;
                assertEquals(xml, token.hasNull);
                assertEquals(xml ? "\0" : "�", token.getData());
            } finally {
                builder.reader.close();
            }
        }
    }
}
