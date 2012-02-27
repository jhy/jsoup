package org.jsoup.parser;

import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests XmlTreeBuilder.
 *
 * @author Jonathan Hedley
 */
public class XmlTreeBuilderTest {
    @Test
    public void testSimpleXmlParse() {
        String xml = "<doc id=2 href='/bar'>Foo <br /><link>One</link><link>Two</link></doc>";
        XmlTreeBuilder tb = new XmlTreeBuilder();
        Document doc = tb.parse(xml, "http://foo.com/");
        assertEquals("<doc id=\"2\" href=\"/bar\">Foo <br /><link>One</link><link>Two</link></doc>",
                TextUtil.stripNewlines(doc.html()));
    }
}
