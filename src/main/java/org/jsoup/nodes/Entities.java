package org.jsoup.nodes;

import org.jsoup.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharsetEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML entities, and escape routines.
 * Source: <a href="http://www.w3.org/TR/html5/named-character-references.html#named-character-references">W3C HTML
 * named character references</a>.
 */
public class Entities {
    public enum EscapeMode {
        /** Restricted entities suitable for XHTML output: lt, gt, amp, apos, and quot only. */
        xhtml(xhtmlByVal),
        /** Default HTML output entities. */
        base(baseByVal),
        /** Complete HTML entities. */
        extended(fullByVal);

        private Map<Character, String> map;

        EscapeMode(Map<Character, String> map) {
            this.map = map;
        }

        public Map<Character, String> getMap() {
            return map;
        }
    }

    private static final Map<String, Character> full;
    private static final Map<Character, String> xhtmlByVal;
    private static final Map<String, Character> base;
    private static final Map<Character, String> baseByVal;
    private static final Map<Character, String> fullByVal;
    private static final Pattern unescapePattern = Pattern.compile("&(#(x|X)?([0-9a-fA-F]+)|[a-zA-Z]+\\d*);?");
    private static final Pattern strictUnescapePattern = Pattern.compile("&(#(x|X)?([0-9a-fA-F]+)|[a-zA-Z]+\\d*);");

    private Entities() {}

    /**
     * Check if the input is a known named entity
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity
     */
    public static boolean isNamedEntity(String name) {
        return full.containsKey(name);
    }

    /**
     * Check if the input is a known named entity in the base entity set.
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity in the base set
     * @see #isNamedEntity(String)
     */
    public static boolean isBaseNamedEntity(String name) {
        return base.containsKey(name);
    }

    /**
     * Get the Character value of the named entity
     * @param name named entity (e.g. "lt" or "amp")
     * @return the Character value of the named entity (e.g. '<' or '&')
     */
    public static Character getCharacterByName(String name) {
        return full.get(name);
    }
    
    static String escape(String string, Document.OutputSettings out) {
        return escape(string, out.encoder(), out.escapeMode());
    }

    static String escape(String string, CharsetEncoder encoder, EscapeMode escapeMode) {
        StringBuilder accum = new StringBuilder(string.length() * 2);
        Map<Character, String> map = escapeMode.getMap();

        final int length = string.length();
        for (int offset = 0; offset < length; ) {
            final int codePoint = string.codePointAt(offset);

            // surrogate pairs, split implementation for efficiency on single char common case (saves creating strings, char[]):
            if (codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                final char c = (char) codePoint;
                if (map.containsKey(c))
                    accum.append('&').append(map.get(c)).append(';');
                else if (encoder.canEncode(c))
                    accum.append(c);
                else
                    accum.append("&#x").append(Integer.toHexString(codePoint)).append(';');
            } else {
                final String c = new String(Character.toChars(codePoint));
                if (encoder.canEncode(c))
                    accum.append(c);
                else
                    accum.append("&#x").append(Integer.toHexString(codePoint)).append(';');
            }

            offset += Character.charCount(codePoint);
        }

        return accum.toString();
    }

    static String unescape(String string) {
        return unescape(string, false);
    }

    /**
     * Unescape the input string.
     * @param string
     * @param strict if "strict" (that is, requires trailing ';' char, otherwise that's optional)
     * @return
     */
    static String unescape(String string, boolean strict) {
        return Parser.unescapeEntities(string, strict);
    }

    // xhtml has restricted entities
    private static final Object[][] xhtmlArray = {
            {"quot", 0x00022},
            {"amp", 0x00026},
            {"apos", 0x00027},
            {"lt", 0x0003C},
            {"gt", 0x0003E}
    };

    static {
        xhtmlByVal = new HashMap<Character, String>();
        base = loadEntities("entities-base.properties");  // most common / default
        baseByVal = toCharacterKey(base);
        full = loadEntities("entities-full.properties"); // extended and overblown.
        fullByVal = toCharacterKey(full);

        for (Object[] entity : xhtmlArray) {
            Character c = Character.valueOf((char) ((Integer) entity[1]).intValue());
            xhtmlByVal.put(c, ((String) entity[0]));
        }
    }

    private static Map<String, Character> loadEntities(String filename) {
        Properties properties = new Properties();
        Map<String, Character> entities = new HashMap<String, Character>();
        try {
            InputStream in = Entities.class.getResourceAsStream(filename);
            properties.load(in);
            in.close();
        } catch (IOException e) {
            throw new MissingResourceException("Error loading entities resource: " + e.getMessage(), "Entities", filename);
        }

        for (Map.Entry entry: properties.entrySet()) {
            Character val = Character.valueOf((char) Integer.parseInt((String) entry.getValue(), 16));
            String name = (String) entry.getKey();
            entities.put(name, val);
        }
        return entities;
    }

    private static Map<Character, String> toCharacterKey(Map<String, Character> inMap) {
        Map<Character, String> outMap = new HashMap<Character, String>();
        for (Map.Entry<String, Character> entry: inMap.entrySet()) {
            Character character = entry.getValue();
            String name = entry.getKey();

            if (outMap.containsKey(character)) {
                // dupe, prefer the lower case version
                if (name.toLowerCase().equals(name))
                    outMap.put(character, name);
            } else {
                outMap.put(character, name);
            }
        }
        return outMap;
    }
}
