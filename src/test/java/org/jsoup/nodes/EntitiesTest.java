package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.junit.Test;

import static org.junit.Assert.*;

import java.nio.charset.Charset;

public class EntitiesTest {
    @Test public void escape() {
        String text = "Hello &<> Å å π 新 there ¾ ©";
        String escapedAscii = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.base);
        String escapedAsciiFull = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.extended);
        String escapedAsciiXhtml = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.xhtml);
        String escapedUtfFull = Entities.escape(text, Charset.forName("UTF-8").newEncoder(), Entities.EscapeMode.base);
        String escapedUtfMin = Entities.escape(text, Charset.forName("UTF-8").newEncoder(), Entities.EscapeMode.xhtml);

        assertEquals("Hello &amp;&lt;&gt; &Aring; &aring; &#x3c0; &#x65b0; there &frac34; &copy;", escapedAscii);
        assertEquals("Hello &amp;&lt;&gt; &angst; &aring; &pi; &#x65b0; there &frac34; &copy;", escapedAsciiFull);
        assertEquals("Hello &amp;&lt;&gt; &#xc5; &#xe5; &#x3c0; &#x65b0; there &#xbe; &#xa9;", escapedAsciiXhtml);
        assertEquals("Hello &amp;&lt;&gt; &Aring; &aring; π 新 there &frac34; &copy;", escapedUtfFull);
        assertEquals("Hello &amp;&lt;&gt; Å å π 新 there ¾ ©", escapedUtfMin);
        // odd that it's defined as aring in base but angst in full

        // round trip
        assertEquals(text, Entities.unescape(escapedAscii));
        assertEquals(text, Entities.unescape(escapedAsciiFull));
        assertEquals(text, Entities.unescape(escapedAsciiXhtml));
        assertEquals(text, Entities.unescape(escapedUtfFull));
        assertEquals(text, Entities.unescape(escapedUtfMin));
    }

    @Test public void escapeSupplementaryCharacter(){
        String text = new String(Character.toChars(135361));
        String escapedAscii = Entities.escape(text, Charset.forName("ascii").newEncoder(), Entities.EscapeMode.base);
        assertEquals("&#x210c1;", escapedAscii);
        String escapedUtf = Entities.escape(text, Charset.forName("UTF-8").newEncoder(), Entities.EscapeMode.base);
        assertEquals(text, escapedUtf);
    }

    @Test public void unescape() {
        String text = "Hello &amp;&LT&gt; &reg &angst; &angst &#960; &#960 &#x65B0; there &! &frac34; &copy; &COPY;";
        assertEquals("Hello &<> ® Å &angst π π 新 there &! ¾ © ©", Entities.unescape(text));

        assertEquals("&0987654321; &unknown", Entities.unescape("&0987654321; &unknown"));
    }

    @Test public void strictUnescape() { // for attributes, enforce strict unescaping (must look like &#xxx; , not just &#xxx)
        String text = "Hello &amp= &amp;";
        assertEquals("Hello &amp= &", Entities.unescape(text, true));
        assertEquals("Hello &= &", Entities.unescape(text));
        assertEquals("Hello &= &", Entities.unescape(text, false));
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

    @Test public void letterDigitEntities() {
        String html = "<p>&sup1;&sup2;&sup3;&frac14;&frac12;&frac34;</p>";
        Document doc = Jsoup.parse(html);
        Element p = doc.select("p").first();
        assertEquals("&sup1;&sup2;&sup3;&frac14;&frac12;&frac34;", p.html());
        assertEquals("¹²³¼½¾", p.text());
    }

    @Test public void noSpuriousDecodes() {
        String string = "http://www.foo.com?a=1&num_rooms=1&children=0&int=VA&b=2";
        assertEquals(string, Entities.unescape(string));
    }
}
