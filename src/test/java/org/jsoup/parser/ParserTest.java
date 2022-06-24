package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void testUtf8() throws IOException {
        // testcase for https://github.com/jhy/jsoup/issues/1557. no repro.
        Document parsed = Jsoup.parse(new ByteArrayInputStream("<p>H\u00E9llo, w\u00F6rld!".getBytes(StandardCharsets.UTF_8)), null, "");
        String text = parsed.selectFirst("p").wholeText();
        assertEquals(text, "H\u00E9llo, w\u00F6rld!");
    }
}
