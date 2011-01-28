package org.jsoup.nodes;

import org.junit.Test;

import static org.junit.Assert.*;
import org.jsoup.nodes.Entities;

import java.nio.charset.Charset;

public class EntitiesTest {
    @Test public void escape() {
        String text = "Hello &<> Å å π 新 there ¾";
        String escapedAscii = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.base);
        String escapedAsciiFull = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.extended);
        String escapedAsciiXhtml = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.xhtml);
        String escapedUtf = Entities.escape(text, Charset.forName("UTF-8").newEncoder(), Entities.EscapeMode.base);

        assertEquals("Hello &amp;&lt;&gt; &Aring; &aring; &#960; &#26032; there &frac34;", escapedAscii);
        assertEquals("Hello &amp;&lt;&gt; &angst; &aring; &pi; &#26032; there &frac34;", escapedAsciiFull);
        assertEquals("Hello &amp;&lt;&gt; &#197; &#229; &#960; &#26032; there &#190;", escapedAsciiXhtml);
        assertEquals("Hello &amp;&lt;&gt; &Aring; &aring; π 新 there &frac34;", escapedUtf);
        // odd that it's defined as aring in base but angst in full
    }

    @Test public void unescape() {
        String text = "Hello &amp;&LT&gt; &angst &#960; &#960 &#x65B0; there &! &frac34;";
        assertEquals("Hello &<> Å π π 新 there &! ¾", Entities.unescape(text));

        assertEquals("&0987654321; &unknown", Entities.unescape("&0987654321; &unknown"));
    }

    @Test public void strictUnescape() { // for attributes, enforce strict unescaping (must look like &xxx; , not just &xxx)
        String text = "Hello &mid &amp;";
        assertEquals("Hello &mid &", Entities.unescape(text, true));
        assertEquals("Hello ∣ &", Entities.unescape(text));
        assertEquals("Hello ∣ &", Entities.unescape(text, false));
    }

    
    @Test public void caseSensitive() {
        String unescaped = "Ü ü & &";
        assertEquals("&Uuml; &uuml; &amp; &amp;", Entities.escape(unescaped, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.extended));
        
        String escaped = "&Uuml; &uuml; &amp; &AMP";
        assertEquals("Ü ü & &", Entities.unescape(escaped));
    }
    
    @Test public void quoteReplacements() {
        String escaped = "&#92; &#36;";
        String unescaped = "\\ $";
        
        assertEquals(unescaped, Entities.unescape(escaped));
    }
}
