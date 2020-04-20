package org.jsoup.parser;

import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpecialCaseXMLTest {
    @Test
    public void testSpecialXmlParse() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "    <head>\n" +
                "        <title>normal &lt;3 <script>alert()</script> <b>bold</b>!</title>\n" +
                "    </head>\n" +
                "</html>";
        XmlTreeBuilder tb = new XmlTreeBuilder();
        Document doc = tb.parse(xml, "");
        assertEquals("normal <3 !",
                TextUtil.stripNewlines(doc.title()));

    }
}
