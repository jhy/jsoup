package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.DataUtil;
import org.jsoup.internal.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.jsoup.nodes.Document.OutputSettings.*;
import static org.jsoup.nodes.Entities.EscapeMode.base;
import static org.jsoup.nodes.Entities.EscapeMode.extended;

/**
 * HTML entities, and escape routines. Source: <a href="http://www.w3.org/TR/html5/named-character-references.html#named-character-references">W3C
 * HTML named character references</a>.
 */
public class Entities {
    // constants for escape options:
    static final int ForText = 0x1;
    static final int ForAttribute = 0x2;
    static final int Normalise = 0x4;
    static final int TrimLeading = 0x8;
    static final int TrimTrailing = 0x10;

    private static final int empty = -1;
    private static final String emptyName = "";
    static final int codepointRadix = 36;
    private static final char[] codeDelims = {',', ';'};
    private static final HashMap<String, String> multipoints = new HashMap<>(); // name -> multiple character references

    private static final int BaseCount = 106;
    private static final ArrayList<String> baseSorted = new ArrayList<>(BaseCount); // names sorted longest first, for prefix matching

    public enum EscapeMode {
        /**
         * Restricted entities suitable for XHTML output: lt, gt, amp, and quot only.
         */
        xhtml(EntitiesData.xmlPoints, 4),
        /**
         * Default HTML output entities.
         */
        base(EntitiesData.basePoints, 106),
        /**
         * Complete HTML entities.
         */
        extended(EntitiesData.fullPoints, 2125);

        static {
            // sort the base names by length, for prefix matching
            Collections.addAll(baseSorted, base.nameKeys);
            baseSorted.sort((a, b) -> b.length() - a.length());
        }

        // table of named references to their codepoints. sorted so we can binary search. built by BuildEntities.
        private String[] nameKeys;
        private int[] codeVals; // limitation is the few references with multiple characters; those go into multipoints.

        // table of codepoints to named entities.
        private int[] codeKeys; // we don't support multicodepoints to single named value currently
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
    }

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

    /**
     Finds the longest base named entity that is a prefix of the input. That is, input "notit" would return "not".

     @return longest entity name that is a prefix of the input, or "" if no entity matches
     */
    public static String findPrefix(String input) {
        for (String name : baseSorted) {
            if (input.startsWith(name)) return name;
        }
        return emptyName;
        // if perf critical, could look at using a Trie vs a scan
    }

    /**
     HTML escape an input string. That is, {@code <} is returned as {@code &lt;}. The escaped string is suitable for use
     both in attributes and in text data.
     @param data the un-escaped string to escape
     @param out the output settings to use. This configures the character set escaped against (that is, if a
     character is supported in the output character set, it doesn't have to be escaped), and also HTML or XML
     settings.
     @return the escaped string
     */
    public static String escape(String data, OutputSettings out) {
        return escapeString(data, out.escapeMode(), out.syntax(), out.charset());
    }

    /**
     HTML escape an input string, using the default settings (UTF-8, base entities, HTML syntax). That is, {@code <} is
     returned as {@code &lt;}. The escaped string is suitable for use both in attributes and in text data.
     @param data the un-escaped string to escape
     @return the escaped string
     @see #escape(String, OutputSettings)
     */
    public static String escape(String data) {
        return escapeString(data, base, Syntax.html, DataUtil.UTF_8);
    }

    private static String escapeString(String data, EscapeMode escapeMode, Syntax syntax, Charset charset) {
        if (data == null)
            return "";
        StringBuilder accum = StringUtil.borrowBuilder();
        try {
            doEscape(data, accum, escapeMode, syntax, charset, ForText | ForAttribute);
        } catch (IOException e) {
            throw new SerializationException(e); // doesn't happen
        }
        return StringUtil.releaseBuilder(accum);
    }


    static void escape(Appendable accum, String data, OutputSettings out, int options) throws IOException {
        doEscape(data, accum, out.escapeMode(), out.syntax(), out.charset(), options);
    }

    private static void doEscape(String data, Appendable accum, EscapeMode mode, Syntax syntax, Charset charset, int options) throws IOException {
        final CoreCharset coreCharset = CoreCharset.byName(charset.name());
        final CharsetEncoder fallback = encoderFor(charset);
        final int length = data.length();

        int codePoint;
        boolean lastWasWhite = false;
        boolean reachedNonWhite = false;
        boolean skipped = false;
        for (int offset = 0; offset < length; offset += Character.charCount(codePoint)) {
            codePoint = data.codePointAt(offset);

            if ((options & Normalise) != 0) {
                if (StringUtil.isWhitespace(codePoint)) {
                    if ((options & TrimLeading) != 0 && !reachedNonWhite) continue;
                    if (lastWasWhite) continue;
                    if ((options & TrimTrailing) != 0) {
                        skipped = true;
                        continue;
                    }
                    accum.append(' ');
                    lastWasWhite = true;
                    continue;
                } else {
                    lastWasWhite = false;
                    reachedNonWhite = true;
                    if (skipped) {
                        accum.append(' '); // wasn't the end, so need to place a normalized space
                        skipped = false;
                    }
                }
            }
            appendEscaped(codePoint, accum, options, mode, syntax, coreCharset, fallback);
        }
    }

    private static void appendEscaped(int codePoint, Appendable accum, int options, EscapeMode escapeMode,
        Syntax syntax, CoreCharset coreCharset, CharsetEncoder fallback) throws IOException {

        // surrogate pairs, split implementation for efficiency on single char common case (saves creating strings, char[]):
        final char c = (char) codePoint;
        if (codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // html specific and required escapes:
            switch (c) {
                case '&':
                    accum.append("&amp;");
                    break;
                case 0xA0:
                    appendNbsp(accum, escapeMode);
                    break;
                case '<':
                    // escape when in character data or when in a xml attribute val or XML syntax; not needed in html attr val
                    appendLt(accum, options, escapeMode, syntax);
                    break;
                case '>':
                    if ((options & ForText) != 0) accum.append("&gt;");
                    else accum.append(c);
                    break;
                case '"':
                    if ((options & ForAttribute) != 0) accum.append("&quot;");
                    else accum.append(c);
                    break;
                case '\'':
                    // special case for the Entities.escape(string) method when we are maximally escaping. Otherwise, because we output attributes in "", there's no need to escape.
                    appendApos(accum, options, escapeMode);
                    break;
                // we escape ascii control <x20 (other than tab, line-feed, carriage return) for XML compliance (required) and HTML ease of reading (not required) - https://www.w3.org/TR/xml/#charsets
                case 0x9:
                case 0xA:
                case 0xD:
                    accum.append(c);
                    break;
                default:
                    if (c < 0x20 || !canEncode(coreCharset, c, fallback)) appendEncoded(accum, escapeMode, codePoint);
                    else accum.append(c);
            }
        } else {
            if (canEncode(coreCharset, c, fallback)) {
                // reads into charBuf - we go through these steps to avoid GC objects as much as possible (would be a new String and a new char[2] for each character)
                char[] chars = charBuf.get();
                int len = Character.toChars(codePoint, chars, 0);
                if (accum instanceof StringBuilder) // true unless the user supplied their own
                    ((StringBuilder) accum).append(chars, 0, len);
                else
                    accum.append(new String(chars, 0, len));
            } else {
                appendEncoded(accum, escapeMode, codePoint);
            }
        }
    }

    private static final ThreadLocal<char[]> charBuf = ThreadLocal.withInitial(() -> new char[2]);

    private static void appendNbsp(Appendable accum, EscapeMode escapeMode) throws IOException {
        if (escapeMode != EscapeMode.xhtml) accum.append("&nbsp;");
        else accum.append("&#xa0;");
    }

    private static void appendLt(Appendable accum, int options, EscapeMode escapeMode, Syntax syntax) throws IOException {
        if ((options & ForText) != 0 || escapeMode == EscapeMode.xhtml || syntax == Syntax.xml) accum.append("&lt;");
        else accum.append('<'); // no need to escape < when in an HTML attribute
    }

    private static void appendApos(Appendable accum, int options, EscapeMode escapeMode) throws IOException {
        if ((options & ForAttribute) != 0 && (options & ForText) != 0) {
            if (escapeMode == EscapeMode.xhtml) accum.append("&#x27;");
            else accum.append("&apos;");
        } else {
            accum.append('\'');
        }
    }

    private static void appendEncoded(Appendable accum, EscapeMode escapeMode, int codePoint) throws IOException {
        final String name = escapeMode.nameForCodepoint(codePoint);
        if (!emptyName.equals(name)) // ok for identity check
            accum.append('&').append(name).append(';');
        else
            accum.append("&#x").append(Integer.toHexString(codePoint)).append(';');
    }

    /**
     * Un-escape an HTML escaped string. That is, {@code &lt;} is returned as {@code <}.
     *
     * @param string the HTML string to un-escape
     * @return the unescaped string
     */
    public static String unescape(String string) {
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
                return !(c >= Character.MIN_SURROGATE && c < (Character.MAX_SURROGATE + 1)); // !Character.isSurrogate(c); but not in Android 10 desugar
            default:
                return fallback.canEncode(c);
        }
    }

    enum CoreCharset {
        ascii, utf, fallback;

        static CoreCharset byName(final String name) {
            if (name.equals("US-ASCII"))
                return ascii;
            if (name.startsWith("UTF-")) // covers UTF-8, UTF-16, et al
                return utf;
            return fallback;
        }
    }

    // cache the last used fallback encoder to save recreating on every use
    private static final ThreadLocal<CharsetEncoder> LocalEncoder = new ThreadLocal<>();
    private static CharsetEncoder encoderFor(Charset charset) {
        CharsetEncoder encoder = LocalEncoder.get();
        if (encoder == null || !encoder.charset().equals(charset)) {
            encoder = charset.newEncoder();
            LocalEncoder.set(encoder);
        }
        return encoder;
    }

    private static void load(EscapeMode e, String pointsData, int size) {
        e.nameKeys = new String[size];
        e.codeVals = new int[size];
        e.codeKeys = new int[size];
        e.nameVals = new String[size];

        int i = 0;
        CharacterReader reader = new CharacterReader(pointsData);
        try {
            while (!reader.isEmpty()) {
                // NotNestedLessLess=10913,824;1887&

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
                final String indexS = reader.consumeTo('&');
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

            Validate.isTrue(i == size, "Unexpected count of entities loaded");
        } finally {
            reader.close();
        }
    }
}
