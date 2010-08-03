package org.jsoup.nodes;

import org.junit.Test;

import static org.junit.Assert.*;
import org.jsoup.nodes.Entities;

import java.nio.charset.Charset;

public class EntitiesTest {
    @Test public void escape() {
        String text = "Hello &<> Å π 新 there";
        String escapedAscii = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.base);
        String escapedAsciiFull = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.extended);
        String escapedUtf = Entities.escape(text, Charset.forName("UTF-8").newEncoder(), Entities.EscapeMode.base);

        assertEquals("Hello &amp;&lt;&gt; &aring; &#960; &#26032; there", escapedAscii);
        assertEquals("Hello &amp;&lt;&gt; &angst; &pi; &#26032; there", escapedAsciiFull);
        assertEquals("Hello &amp;&lt;&gt; &aring; π 新 there", escapedUtf);
        // odd that it's defined as aring in base but angst in full
    }

    @Test public void unescape() {
        String text = "Hello &amp;&LT&gt; &ANGST &#960; &#960 &#x65B0; there &!";
        assertEquals("Hello &<> Å π π 新 there &!", Entities.unescape(text));

        assertEquals("&0987654321; &unknown", Entities.unescape("&0987654321; &unknown"));
    }
}
