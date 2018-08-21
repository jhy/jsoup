package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttributeTest {
    @Test public void html() {
        Attribute attr = new Attribute("key", "value &", Attribute.QuoteType.DOUBLE_QUOTED);
        assertEquals("key=\"value &amp;\"", attr.html());
        assertEquals(attr.html(), attr.toString());
    }

    @Test public void testWithSupplementaryCharacterInAttributeKeyAndValue() {
        String s = new String(Character.toChars(135361));
        Attribute attr = new Attribute(s, "A" + s + "B", Attribute.QuoteType.DOUBLE_QUOTED);
        assertEquals(s + "=\"A" + s + "B\"", attr.html());
        assertEquals(attr.html(), attr.toString());
    }

    @Test public void testParseUnquotedAttributeValue() {
        Document doc = Jsoup.parse("<html><head></head><body><a name=test>...</a></body></html>");

        Attributes attr = doc.select("a").first().attributes();
        assertEquals(Attribute.QuoteType.UNQUOTED, attr.getQuoteType("name"));
    }

    @Test public void testParseSingleQuotedAttributeValue() {
        Document doc = Jsoup.parse("<html><head></head><body><a name='test'>...</a></body></html>");

        Attributes attr = doc.select("a").first().attributes();
        assertEquals(Attribute.QuoteType.SINGLE_QUOTED, attr.getQuoteType("name"));
    }

    @Test public void testParseDoubleQuotedAttributeValue() {
        Document doc = Jsoup.parse("<html><head></head><body><a name=\"test\">...</a></body></html>");

        Attributes attr = doc.select("a").first().attributes();
        assertEquals(Attribute.QuoteType.DOUBLE_QUOTED, attr.getQuoteType("name"));
    }
}
