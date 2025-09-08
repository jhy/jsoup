package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ParserTest {

    @Test
    public void unescapeEntities() {
        String s = Parser.unescapeEntities("One &amp; Two", false);
        assertEquals("One & Two", s);
    }

    @Test
    public void unescapeEntitiesHandlesLargeInput() {
        StringBuilder longBody = new StringBuilder(500000);
        do {
            longBody.append("SomeNonEncodedInput");
        } while (longBody.length() < 64 * 1024);

        String body = longBody.toString();
        assertEquals(body, Parser.unescapeEntities(body, false));
    }

    @Test public void unescapeTracksErrors() {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(10);

        String s = parser.unescape("One &bogus; &amp; &gt Two", false);
        assertEquals("One &bogus; & > Two", s);
        ParseErrorList errors = parser.getErrors();
        assertEquals(2, errors.size());
        assertEquals("<1:6>: Invalid character reference: invalid named reference [bogus]", errors.get(0).toString());
        assertEquals("<1:22>: Invalid character reference: missing semicolon on [&gt]", errors.get(1).toString());

        // can reuse parser; errors will be reset
        s = parser.unescape("One &amp; &bogus; Two", false);
        assertEquals("One & &bogus; Two", s);
        assertEquals(1, parser.getErrors().size());
        assertEquals("<1:12>: Invalid character reference: invalid named reference [bogus]", parser.getErrors().get(0).toString());
    }

    @Test
    public void testUtf8() throws IOException {
        // testcase for https://github.com/jhy/jsoup/issues/1557. no repro.
        Document parsed = Jsoup.parse(new ByteArrayInputStream("<p>H\u00E9llo, w\u00F6rld!".getBytes(StandardCharsets.UTF_8)), null, "");
        String text = parsed.selectFirst("p").wholeText();
        assertEquals(text, "H\u00E9llo, w\u00F6rld!");
    }

    @Test
    public void testClone() {
        // Test HTML parser cloning
        Parser htmlParser = Parser.htmlParser();
        Parser htmlClone = htmlParser.clone();
        assertNotSame(htmlParser, htmlClone);
        // Ensure the tree builder instances are different
        assertNotSame(htmlParser.getTreeBuilder(), htmlClone.getTreeBuilder());
        // Check that settings are cloned properly (for example, tag case settings)
        assertEquals(htmlParser.settings().preserveTagCase(), htmlClone.settings().preserveTagCase());
        assertEquals(htmlParser.settings().preserveAttributeCase(), htmlClone.settings().preserveAttributeCase());

        // Test XML parser cloning
        Parser xmlParser = Parser.xmlParser();
        Parser xmlClone = xmlParser.clone();
        assertNotSame(xmlParser, xmlClone);
        assertNotSame(xmlParser.getTreeBuilder(), xmlClone.getTreeBuilder());
        assertEquals(xmlParser.settings().preserveTagCase(), xmlClone.settings().preserveTagCase());
        assertEquals(xmlParser.settings().preserveAttributeCase(), xmlClone.settings().preserveAttributeCase());
    }
}
