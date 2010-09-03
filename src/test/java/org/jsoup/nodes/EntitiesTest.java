package org.jsoup.nodes;

import org.junit.Test;

import static org.junit.Assert.*;
import org.jsoup.nodes.Entities;

import java.nio.charset.Charset;

public class EntitiesTest {
    @Test public void escape() {
        String text = "Hello &<> Å å π 新 there";
        String escapedAscii = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.base);
        String escapedAsciiFull = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.extended);
        String escapedAsciiXhtml = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.xhtml);
        String escapedUtf = Entities.escape(text, Charset.forName("UTF-8").newEncoder(), Entities.EscapeMode.base);

        assertEquals("Hello &amp;&lt;&gt; &Aring; &aring; &#960; &#26032; there", escapedAscii);
        assertEquals("Hello &amp;&lt;&gt; &angst; &aring; &pi; &#26032; there", escapedAsciiFull);
        assertEquals("Hello &amp;&lt;&gt; &#197; &#229; &#960; &#26032; there", escapedAsciiXhtml);
        assertEquals("Hello &amp;&lt;&gt; &Aring; &aring; π 新 there", escapedUtf);
        // odd that it's defined as aring in base but angst in full
    }

    @Test public void unescape() {
        String text = "Hello &amp;&LT&gt; &angst &#960; &#960 &#x65B0; there &!";
        assertEquals("Hello &<> Å π π 新 there &!", Entities.unescape(text));

        assertEquals("&0987654321; &unknown", Entities.unescape("&0987654321; &unknown"));
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
