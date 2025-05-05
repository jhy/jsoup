package org.jsoup;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationExceptionTest {
    @Test void constructors() {
        SerializationException e = new SerializationException("message");
        assertEquals("message", e.getMessage());
        assertNull(e.getCause());

        SerializationException e2 = new SerializationException("message", new Exception("cause"));
        assertEquals("message", e2.getMessage());
        assertEquals("cause", e2.getCause().getMessage());

        SerializationException e3 = new SerializationException();
        assertNull(e3.getMessage());
        assertNull(e3.getCause());
    }

    private Appendable brokenAppender() {
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
        assertThrows(SerializationException.class, () -> doc.html(brokenWriter));
    }
}
