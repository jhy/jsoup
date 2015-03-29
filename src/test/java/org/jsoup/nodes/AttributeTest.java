package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.JsoupOptions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttributeTest {
    @Test public void html() {
        Attribute attr = new Attribute("key", "value &");
        assertEquals("key=\"value &amp;\"", attr.html());
        assertEquals(attr.html(), attr.toString());
    }

    @Test public void testWithSupplementaryCharacterInAttributeKeyAndValue() {
        String s = new String(Character.toChars(135361));
        Attribute attr = new Attribute(s, "A" + s + "B");
        assertEquals(s + "=\"A" + s + "B\"", attr.html());
        assertEquals(attr.html(), attr.toString());
    }

    @Test public void testNotNormalizingAttributes() {
        JsoupOptions oldOptions = Jsoup.options();
        try {
          Jsoup.options(new JsoupOptions.Builder().normalizeAttributes(false).build());
          String s = "showCenterMarker";
          Attribute attr = new Attribute(s, "somevalue");
          assertEquals(s + "=\"somevalue\"", attr.html());
          assertEquals(attr.html(), attr.toString());
        } finally {
          // Make sure we reset normalizing attributes even if the test fails
          Jsoup.options(oldOptions);
        }
    }
}
