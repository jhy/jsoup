package org.jsoup.internal;

import org.jsoup.Jsoup;
import org.jsoup.SerializationException;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.CharArrayWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class QuietAppendableTest {
    @Test void wrap() {
        assertInstanceOf(
            QuietAppendable.StringBuilderAppendable.class,
            QuietAppendable.wrap(new StringBuilder())
        );

        assertInstanceOf(
            QuietAppendable.BaseAppendable.class,
            QuietAppendable.wrap(new CharArrayWriter())
        );
    }

    @Test void supplemental() {
        // hits append(char[] chars, int offset, int len) with len 2 for supplemental codepoint
        String expect = "😀";
        char[] chars = new char[2];
        chars[0] = expect.charAt(0);
        chars[1] = expect.charAt(1);
        assertEquals(2, expect.length());

        QuietAppendable sb = QuietAppendable.wrap(new StringBuilder());
        sb.append(chars, 0, 2);
        String s = sb.toString();
        assertEquals(expect, s);

        CharArrayWriter cw = new CharArrayWriter();
        QuietAppendable qa = QuietAppendable.wrap(cw);
        qa.append(chars, 0, 2);
        String out = cw.toString();
        assertEquals(expect, out);
    }

    private static Appendable brokenAppender() {
        // returns an Appendable that throws an IOException on any put
        return new Appendable() {
            @Override
            public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("broken");
            }

            @Override
            public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("broken");
            }

            @Override
            public Appendable append(char c) throws IOException {
                throw new IOException("broken");
            }
        };
    }

    @Test void appendThrowsSerializationException() {
        Document doc = Jsoup.parse("<div>");
        Appendable brokenWriter = brokenAppender();
        boolean threw = false;
        try {
            doc.html(brokenWriter);
        } catch (SerializationException e) {
            threw = true;
            Throwable cause = e.getCause();
            assertEquals("broken", cause.getMessage());
            assertInstanceOf(IOException.class, cause);
        }
        assertTrue(threw);
    }

    // Lucas
    @Test void quietAppendNotNullThreeParam() {
        String expect = "😀";
        char[] chars = new char[2];
        chars[0] = expect.charAt(0);
        chars[1] = expect.charAt(1);
        assertEquals(2, expect.length());

        QuietAppendable sb = QuietAppendable.wrap(new StringBuilder());
        assertNotNull(sb.append(chars, 0, 2));
    }

    // Handles non-nullity of append(char[], int, int) override as well
    @Test
    public void testBaseAppendableChaining() {

        Appendable accum = new StringBuilder();
        QuietAppendable qa = QuietAppendable.wrap(accum);

        char[] chars = {'h', 'e', 'l', 'l', 'o'};

        QuietAppendable result = qa.append(chars, 0, 5).append('!');
        assertNotNull(result);
        assertEquals("hello!", accum.toString());

        // let's do the same with a StringBuilder type too
        StringBuilder accum2 = new StringBuilder();
        QuietAppendable qa2 = QuietAppendable.wrap(accum2);
        assertNotNull(qa2.append(chars, 0, 5).append('!'));
        assertEquals("hello!", accum2.toString());
    }
}
