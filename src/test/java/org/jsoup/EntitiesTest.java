package org.jsoup;

import org.junit.Test;

import static org.junit.Assert.*;

import java.nio.charset.Charset;

public class EntitiesTest {
    @Test public void simpleEscape() {
        String text = "Hello &<> Å π 新 there";
        String escapedAscii = Entities.escape(text, Charset.forName("ascii"));
        String escapedUtf = Entities.escape(text, Charset.forName("UTF-8"));

        assertEquals("Hello &amp;&lt;&gt; &angst; &pi; &#26032; there", escapedAscii);
        assertEquals("Hello &amp;&lt;&gt; &angst; &pi; 新 there", escapedUtf);
    }
}
