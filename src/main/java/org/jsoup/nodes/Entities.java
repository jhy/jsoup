package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.DataUtil;
import org.jsoup.helper.StringUtil;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.HashMap;

import static org.jsoup.nodes.Entities.EscapeMode.base;
import static org.jsoup.nodes.Entities.EscapeMode.extended;

/**
 * HTML entities, and escape routines.
 * Source: <a href="http://www.w3.org/TR/html5/named-character-references.html#named-character-references">W3C HTML
 * named character references</a>.
 */
public class Entities {
    private static final int empty = -1;
    private static final String emptyName = "";
    static final int codepointRadix = 36;

    public enum EscapeMode {
        /**
         * Restricted entities suitable for XHTML output: lt, gt, amp, and quot only.
         */
        xhtml("entities-xhtml.properties", 4),
        /**
         * Default HTML output entities.
         */
        base("entities-base.properties", 106),
        /**
         * Complete HTML entities.
         */
        extended("entities-full.properties", 2125);

        // table of named references to their codepoints. sorted so we can binary search. built by BuildEntities.
        private String[] nameKeys;
        private int[] codeVals; // limitation is the few references with multiple characters; those go into multipoints.

        // table of codepoints to named entities.
        private int[] codeKeys; // we don' support multicodepoints to single named value currently
        private String[] nameVals;

        EscapeMode(String file, int size) {
            load(this, file, size);
        }

        int codepointForName(final String name) {
            int index = Arrays.binarySearch(nameKeys, name);
            return index >= 0 ? codeVals[index] : empty;
        }

        String nameForCodepoint(final int codepoint) {
            final int index = Arrays.binarySearch(codeKeys, codepoint);
            if (index >= 0) {
                // the results are ordered so lower case versions of same codepoint come after uppercase, and we prefer to emit lower
                // (and binary search for same item with multi results is undefined
                return (index < nameVals.length - 1 && codeKeys[index + 1] == codepoint) ?
                    nameVals[index + 1] : nameVals[index];
            }
            return emptyName;
        }

        private int size() {
            return nameKeys.length;
        }
    }

    private static final HashMap<String, String> multipoints = new HashMap<String, String>(); // name -> multiple character references

    private Entities() {
    }

    /**
     * Check if the input is a known named entity
     *
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity
     */
    public static boolean isNamedEntity(final String name) {
        return extended.codepointForName(name) != empty;
    }

    /**
     * Check if the input is a known named entity in the base entity set.
     *
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity in the base set
     * @see #isNamedEntity(String)
     */
    public static boolean isBaseNamedEntity(final String name) {
        return base.codepointForName(name) != empty;
    }

    /**
     * Get the Character value of the named entity
     *
     * @param name named entity (e.g. "lt" or "amp")
     * @return the Character value of the named entity (e.g. '{@literal <}' or '{@literal &}')
     * @deprecated does not support characters outside the BMP or multiple character names
     */
    public static Character getCharacterByName(String name) {
        return (char) extended.codepointForName(name);
    }

    /**
     * Get the character(s) represented by the named entity
     *
     * @param name entity (e.g. "lt" or "amp")
     * @return the string value of the character(s) represented by this entity, or "" if not defined
     */
    public static String getByName(String name) {
        String val = multipoints.get(name);
        if (val != null)
            return val;
        int codepoint = extended.codepointForName(name);
        if (codepoint != empty)
            return new String(new int[]{codepoint}, 0, 1);
        return emptyName;
    }

    public static int codepointsForName(final String name, final int[] codepoints) {
        String val = multipoints.get(name);
        if (val != null) {
            codepoints[0] = val.codePointAt(0);
            codepoints[1] = val.codePointAt(1);
            return 2;
        }
        int codepoint = extended.codepointForName(name);
        if (codepoint != empty) {
            codepoints[0] = codepoint;
            return 1;
        }
        return 0;
    }

    static String escape(String string, Document.OutputSettings out) {
        StringBuilder accum = new StringBuilder(string.length() * 2);
        try {
            escape(accum, string, out, false, false, false);
        } catch (IOException e) {
            throw new SerializationException(e); // doesn't happen
        }
        return accum.toString();
    }

    // this method is ugly, and does a lot. but other breakups cause rescanning and stringbuilder generations
    static void escape(Appendable accum, String string, Document.OutputSettings out,
                       boolean inAttribute, boolean normaliseWhite, boolean stripLeadingWhite) throws IOException {

        boolean lastWasWhite = false;
        boolean reachedNonWhite = false;
        final EscapeMode escapeMode = out.escapeMode();
        final CharsetEncoder encoder = out.encoder();
        final CoreCharset coreCharset = CoreCharset.byName(encoder.charset().name());
        final int length = string.length();

        int codePoint;
        for (int offset = 0; offset < length; offset += Character.charCount(codePoint)) {
            codePoint = string.codePointAt(offset);

            if (normaliseWhite) {
                if (StringUtil.isWhitespace(codePoint)) {
                    if ((stripLeadingWhite && !reachedNonWhite) || lastWasWhite)
                        continue;
                    accum.append(' ');
                    lastWasWhite = true;
                    continue;
                } else {
                    lastWasWhite = false;
                    reachedNonWhite = true;
                }
            }
            // surrogate pairs, split implementation for efficiency on single char common case (saves creating strings, char[]):
            if (codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                final char c = (char) codePoint;
                // html specific and required escapes:
                switch (c) {
                    case '&':
                        accum.append("&amp;");
                        break;
                    case 0xA0:
                        if (escapeMode != EscapeMode.xhtml)
                            accum.append("&nbsp;");
                        else
                            accum.append("&#xa0;");
                        break;
                    case '<':
                        // escape when in character data or when in a xml attribue val; not needed in html attr val
                        if (!inAttribute || escapeMode == EscapeMode.xhtml)
                            accum.append("&lt;");
                        else
                            accum.append(c);
                        break;
                    case '>':
                        if (!inAttribute)
                            accum.append("&gt;");
                        else
                            accum.append(c);
                        break;
                    case '"':
                        if (inAttribute)
                            accum.append("&quot;");
                        else
                            accum.append(c);
                        break;
                    default:
                        if (canEncode(coreCharset, c, encoder))
                            accum.append(c);
                        else
                            appendEncoded(accum, escapeMode, codePoint);
                }
            } else {
                final String c = new String(Character.toChars(codePoint));
                if (encoder.canEncode(c)) // uses fallback encoder for simplicity
                    accum.append(c);
                else
                    appendEncoded(accum, escapeMode, codePoint);
            }
        }
    }

    private static void appendEncoded(Appendable accum, EscapeMode escapeMode, int codePoint) throws IOException {
        final String name = escapeMode.nameForCodepoint(codePoint);
        if (name != emptyName) // ok for identity check
            accum.append('&').append(name).append(';');
        else
            accum.append("&#x").append(Integer.toHexString(codePoint)).append(';');
    }

    static String unescape(String string) {
        return unescape(string, false);
    }

    /**
     * Unescape the input string.
     *
     * @param string to un-HTML-escape
     * @param strict if "strict" (that is, requires trailing ';' char, otherwise that's optional)
     * @return unescaped string
     */
    static String unescape(String string, boolean strict) {
        return Parser.unescapeEntities(string, strict);
    }

    /*
     * Provides a fast-path for Encoder.canEncode, which drastically improves performance on Android post JellyBean.
     * After KitKat, the implementation of canEncode degrades to the point of being useless. For non ASCII or UTF,
     * performance may be bad. We can add more encoders for common character sets that are impacted by performance
     * issues on Android if required.
     *
     * Benchmarks:     *
     * OLD toHtml() impl v New (fastpath) in millis
     * Wiki: 1895, 16
     * CNN: 6378, 55
     * Alterslash: 3013, 28
     * Jsoup: 167, 2
     */
    private static boolean canEncode(final CoreCharset charset, final char c, final CharsetEncoder fallback) {
        // todo add more charset tests if impacted by Android's bad perf in canEncode
        switch (charset) {
            case ascii:
                return c < 0x80;
            case utf:
                return true; // real is:!(Character.isLowSurrogate(c) || Character.isHighSurrogate(c)); - but already check above
            default:
                return fallback.canEncode(c);
        }
    }

    private enum CoreCharset {
        ascii, utf, fallback;

        private static CoreCharset byName(String name) {
            if (name.equals("US-ASCII"))
                return ascii;
            if (name.startsWith("UTF-")) // covers UTF-8, UTF-16, et al
                return utf;
            return fallback;
        }
    }

    private static final char[] codeDelims = {',', ';'};

    private static void load(EscapeMode e, String file, int size) {
        e.nameKeys = new String[size];
        e.codeVals = new int[size];
        e.codeKeys = new int[size];
        e.nameVals = new String[size];

        InputStream stream = Entities.class.getResourceAsStream(file);
        if (stream == null)
            throw new IllegalStateException("Could not read resource " + file + ". Make sure you copy resources for " + Entities.class.getCanonicalName());

        int i = 0;
        try {
            ByteBuffer bytes = DataUtil.readToByteBuffer(stream, 0);
            String contents = Charset.forName("ascii").decode(bytes).toString();
            CharacterReader reader = new CharacterReader(contents);

            while (!reader.isEmpty()) {
                // NotNestedLessLess=10913,824;1887

                final String name = reader.consumeTo('=');
                reader.advance();
                final int cp1 = Integer.parseInt(reader.consumeToAny(codeDelims), codepointRadix);
                final char codeDelim = reader.current();
                reader.advance();
                final int cp2;
                if (codeDelim == ',') {
                    cp2 = Integer.parseInt(reader.consumeTo(';'), codepointRadix);
                    reader.advance();
                } else {
                    cp2 = empty;
                }
                String indexS = reader.consumeTo('\n');
                // default git checkout on windows will add a \r there, so remove
                if (indexS.charAt(indexS.length() - 1) == '\r') {
                    indexS = indexS.substring(0, indexS.length() - 1);
                }
                final int index = Integer.parseInt(indexS, codepointRadix);
                reader.advance();

                e.nameKeys[i] = name;
                e.codeVals[i] = cp1;
                e.codeKeys[index] = cp1;
                e.nameVals[index] = name;

                if (cp2 != empty) {
                    multipoints.put(name, new String(new int[]{cp1, cp2}, 0, 2));
                }
                i++;


            }
        } catch (IOException err) {
            throw new IllegalStateException("Error reading resource " + file);
        }
    }
}
