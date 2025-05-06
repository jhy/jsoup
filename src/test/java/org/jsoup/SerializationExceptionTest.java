package org.jsoup;

import org.junit.jupiter.api.Test;

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
}
