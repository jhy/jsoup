package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.StringUtil;
import org.jsoup.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jsoup.nodes.Entities.EscapeMode.base;
import static org.jsoup.nodes.Entities.EscapeMode.extended;

/**
 * HTML entities, and escape routines.
 * Source: <a href="http://www.w3.org/TR/html5/named-character-references.html#named-character-references">W3C HTML
 * named character references</a>.
 */
public class Entities {
    private static Pattern entityPattern = Pattern.compile("^(\\w+)=(\\w+)(?:,(\\w+))?;(\\w+)$");
    private static final int empty = -1;
    private static final String emptyName = "";
    static final int codepointRadix = 36;

    public enum EscapeMode {

        /** Restricted entities suitable for XHTML output: lt, gt, amp, and quot only. */
        xhtml(new XhtmlCharacterCodeTable()),
        /** Default HTML output entities. */
        base(new BaseCharacterCodeTable()),
        /** Complete HTML entities. */
        extended(new FullCharacterCodeTable());

        final CharacterCodeTable table;

        EscapeMode(final CharacterCodeTable table) {
            this.table = table;
        }

        int codepointForName(final String name) {
            return table.codepontForName(name);
        }

        String nameForCodepoint(final int codepoint) {
            return table.nameForCodepoint(codepoint);
        }
    }

    private static final HashMap<String, String> multipoints = new HashMap<String, String>(); // name -> multiple character references

    private Entities() {
    }

    /**
     * Check if the input is a known named entity
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity
     */
    public static boolean isNamedEntity(final String name) {
        return extended.codepointForName(name) != empty;
    }

    /**
     * Check if the input is a known named entity in the base entity set.
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity in the base set
     * @see #isNamedEntity(String)
     */
    public static boolean isBaseNamedEntity(final String name) {
        return base.codepointForName(name) != empty;
    }

    /**
     * Get the Character value of the named entity
     * @param name named entity (e.g. "lt" or "amp")
     * @return the Character value of the named entity (e.g. '{@literal <}' or '{@literal &}')
     * @deprecated does not support characters outside the BMP or multiple character names
     */
    public static Character getCharacterByName(String name) {
        return (char) extended.codepointForName(name);
    }

    /**
     * Get the character(s) represented by the named entitiy
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


    private static abstract class CharacterCodeTable {
        // table of named references to their codepoints. sorted so we can binary search. built by BuildEntities.
        String[] nameKeys = new String[] {};
        int[] codeVals = new int[] {}; // limitation is the few references with multiple characters; those go into multipoints.

        // table of codepoints to named entities.
        int[] codeKeys = new int[] {}; // we don' support multicodepoints to single named value currently
        String[] nameVals = new String[] {};

        int codepontForName(String name) {
            int index = Arrays.binarySearch(nameKeys, name);
            return index >= 0 ? codeVals[index] : empty;
        }

        String nameForCodepoint(int codepoint) {
            final int index = Arrays.binarySearch(codeKeys, codepoint);
            if (index >= 0) {
                // the results are ordered so lower case versions of same codepoint come after uppercase, and we prefer to emit lower
                // (and binary search for same item with multi results is undefined
                return (index < nameVals.length-1 && codeKeys[index+1] == codepoint) ?
                        nameVals[index+1] : nameVals[index];
            }
            return emptyName;
        }
    }

    private static class BaseCharacterCodeTable extends CharacterCodeTable {
        BaseCharacterCodeTable() {
            fillValues();
        }

        private void fillValues() {
            nameKeys = new String[] {"AElig", "AMP", "Aacute", "Acirc", "Agrave", "Aring", "Atilde", "Auml", "COPY",
                    "Ccedil", "ETH", "Eacute", "Ecirc", "Egrave", "Euml", "GT", "Iacute", "Icirc", "Igrave", "Iuml",
                    "LT", "Ntilde", "Oacute", "Ocirc", "Ograve", "Oslash", "Otilde", "Ouml", "QUOT", "REG", "THORN",
                    "Uacute", "Ucirc", "Ugrave", "Uuml", "Yacute", "aacute", "acirc", "acute", "aelig", "agrave", "amp",
                    "aring", "atilde", "auml", "brvbar", "ccedil", "cedil", "cent", "copy", "curren", "deg", "divide",
                    "eacute", "ecirc", "egrave", "eth", "euml", "frac12", "frac14", "frac34", "gt", "iacute", "icirc",
                    "iexcl", "igrave", "iquest", "iuml", "laquo", "lt", "macr", "micro", "middot", "nbsp", "not",
                    "ntilde", "oacute", "ocirc", "ograve", "ordf", "ordm", "oslash", "otilde", "ouml", "para", "plusmn",
                    "pound", "quot", "raquo", "reg", "sect", "shy", "sup1", "sup2", "sup3", "szlig", "thorn", "times",
                    "uacute", "ucirc", "ugrave", "uml", "uuml", "yacute", "yen", "yuml"};
            codeVals = new int[] {198, 38, 193, 194, 192, 197, 195, 196, 169, 199, 208, 201, 202, 200, 203, 62, 205, 206,
                    204, 207, 60, 209, 211, 212, 210, 216, 213, 214, 34, 174, 222, 218, 219, 217, 220, 221, 225, 226,
                    180, 230, 224, 38, 229, 227, 228, 166, 231, 184, 162, 169, 164, 176, 247, 233, 234, 232, 240, 235,
                    189, 188, 190, 62, 237, 238, 161, 236, 191, 239, 171, 60, 175, 181, 183, 160, 172, 241, 243, 244,
                    242, 170, 186, 248, 245, 246, 182, 177, 163, 34, 187, 174, 167, 173, 185, 178, 179, 223, 254, 215,
                    250, 251, 249, 168, 252, 253, 165, 255};

            codeKeys = new int[] {34, 34, 38, 38, 60, 60, 62, 62, 160, 161, 162, 163, 164, 165, 166 ,167, 168, 169, 169,
                    170, 171, 172, 173, 174, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188,
                    189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208,
                    209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228,
                    229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248,
                    249, 250, 251, 252, 253, 254, 255};
            nameVals = new String[] {"QUOT", "quot", "AMP", "amp", "LT", "lt", "GT", "gt", "nbsp", "iexcl", "cent",
                    "pound", "curren", "yen", "brvbar", "sect", "uml", "COPY", "copy", "ordf", "laquo", "not", "shy",
                    "REG", "reg", "macr", "deg", "plusmn", "sup2", "sup3", "acute", "micro", "para", "middot", "cedil",
                    "sup1", "ordm", "raquo", "frac14", "frac12", "frac34", "iquest", "Agrave", "Aacute", "Acirc",
                    "Atilde", "Auml", "Aring", "AElig", "Ccedil", "Egrave", "Eacute", "Ecirc", "Euml", "Igrave",
                    "Iacute", "Icirc", "Iuml", "ETH", "Ntilde", "Ograve", "Oacute", "Ocirc", "Otilde", "Ouml", "times",
                    "Oslash", "Ugrave", "Uacute", "Ucirc", "Uuml", "Yacute", "THORN", "szlig", "agrave", "aacute",
                    "acirc", "atilde", "auml", "aring", "aelig", "ccedil", "egrave", "eacute", "ecirc", "euml",
                    "igrave", "iacute", "icirc", "iuml", "eth", "ntilde", "ograve", "oacute", "ocirc", "otilde",
                    "ouml", "divide", "oslash", "ugrave", "uacute", "ucirc", "uuml", "yacute", "thorn", "yuml"};
        }
    }

    private static class FullCharacterCodeTable extends CharacterCodeTable {
        private static final int SIZE = 2125;

        FullCharacterCodeTable() {
            load("entities-full.properties", SIZE);
        }

        private void load(String file, int size) {
            nameKeys = new String[size];
            codeVals = new int[size];
            codeKeys = new int[size];
            nameVals = new String[size];

            InputStream stream = Entities.class.getResourceAsStream(file);
            if (stream == null)
                throw new IllegalStateException("Could not read resource " + file + ". Make sure you copy resources for " + Entities.class.getCanonicalName());
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String entry;
            int i = 0;
            try {
                while ((entry = reader.readLine()) != null) {
                    // NotNestedLessLess=10913,824;1887
                    final Matcher match = entityPattern.matcher(entry);
                    if (match.find()) {
                        final String name = match.group(1);
                        final int cp1 = Integer.parseInt(match.group(2), codepointRadix);
                        final int cp2 = match.group(3) != null ? Integer.parseInt(match.group(3), codepointRadix) : empty;
                        final int index = Integer.parseInt(match.group(4), codepointRadix);

                        nameKeys[i] = name;
                        codeVals[i] = cp1;
                        codeKeys[index] = cp1;
                        nameVals[index] = name;

                        if (cp2 != empty) {
                            multipoints.put(name, new String(new int[]{cp1, cp2}, 0, 2));
                        }
                        i++;
                    }

                }
                reader.close();
            } catch (IOException err) {
                throw new IllegalStateException("Error reading resource " + file);
            }
        }
    }

    private static class XhtmlCharacterCodeTable extends CharacterCodeTable {
        XhtmlCharacterCodeTable() {
            fillValues();
        }

        private void fillValues() {
            nameKeys = new String[] {"amp", "gt", "lt", "quot"};
            codeVals = new int[] {38, 62, 60, 34};

            codeKeys = new int[] {34, 38, 60, 62};
            nameVals = new String[] {"quot", "amp", "lt", "gt"};
        }
    }
}
