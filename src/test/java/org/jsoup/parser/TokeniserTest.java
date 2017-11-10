package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TokeniserTest {
    @Test
    public void bufferUpInAttributeVal() {
        // https://github.com/jhy/jsoup/issues/967

        // check each double, singlem, unquoted impls
        String[] quotes = {"\"", "'", ""};
        for (String quote : quotes) {
            String preamble = "<img src=" + quote;
            String tail = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
            StringBuilder sb = new StringBuilder(preamble);

            final int charsToFillBuffer = CharacterReader.maxBufferLen - preamble.length();
            for (int i = 0; i < charsToFillBuffer; i++) {
                sb.append('a');
            }

            sb.append('X'); // First character to cross character buffer boundary
            sb.append(tail + quote + ">\n");

            String html = sb.toString();
            Document doc = Jsoup.parse(html);
            String src = doc.select("img").attr("src");

            assertTrue("Handles for quote " + quote, src.contains("X"));
            assertTrue(src.contains(tail));
        }
    }
}
