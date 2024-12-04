package org.jsoup.parser;

import org.jsoup.UncheckedIOException;
import org.jsoup.helper.Validate;
import org.jsoup.internal.SoftPool;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 CharacterReader consumes tokens off a string. Used internally by jsoup. API subject to changes.
 */
public final class CharacterReader {
    static final char EOF = (char) -1;
    private static final int MaxStringCacheLen = 12;
    private static final int StringCacheSize = 512;
    private String[] stringCache; // holds reused strings in this doc, to lessen garbage
    private static final SoftPool<String[]> StringPool = new SoftPool<>(() -> new String[StringCacheSize]); // reuse cache between iterations

    static final int BufferSize = 1024 * 2;         // visible for testing
    static final int RefillPoint = BufferSize / 2;  // when bufPos characters read, refill; visible for testing
    private static final int RewindLimit = 1024;    // the maximum we can rewind. No HTML entities can be larger than this.

    private Reader reader;      // underlying Reader, will be backed by a buffered+controlled input stream, or StringReader
    private char[] charBuf;     // character buffer we consume from; filled from Reader
    private int bufPos;         // position in charBuf that's been consumed to
    private int bufLength;      // the num of characters actually buffered in charBuf, <= charBuf.length
    private int fillPoint = 0;  // how far into the charBuf we read before re-filling. 0.5 of charBuf.length after bufferUp
    private int consumed;       // how many characters total have been consumed from this CharacterReader (less the current bufPos)
    private int bufMark = -1;   // if not -1, the marked rewind position
    private boolean readFully;  // if the underlying stream has been completely read, no value in further buffering

    private static final SoftPool<char[]> BufferPool = new SoftPool<>(() -> new char[BufferSize]); // recycled char buffer

    @Nullable private ArrayList<Integer> newlinePositions = null; // optionally track the pos() position of newlines - scans during bufferUp()
    private int lineNumberOffset = 1; // line numbers start at 1; += newlinePosition[indexof(pos)]

    public CharacterReader(Reader input, int sz) {
        this(input); // sz is no longer used
    }

    public CharacterReader(Reader input) {
        Validate.notNull(input);
        reader = input;
        charBuf = BufferPool.borrow();
        stringCache = StringPool.borrow();
        bufferUp();
    }

    public CharacterReader(String input) {
        this(new StringReader(input));
    }

    public void close() {
        if (reader == null)
            return;
        try {
            reader.close();
        } catch (IOException ignored) {
        } finally {
            reader = null;
            Arrays.fill(charBuf, (char) 0); // before release, clear the buffer. Not required, but acts as a safety net, and makes debug view clearer
            BufferPool.release(charBuf);
            charBuf = null;
            StringPool.release(stringCache); // conversely, we don't clear the string cache, so we can reuse the contents
            stringCache = null;
        }
    }

    private void bufferUp() {
        if (readFully || bufPos < fillPoint || bufMark != -1)
            return;
        doBufferUp(); // structured so bufferUp may become an intrinsic candidate
    }

    private void doBufferUp() {
        /*
        The flow:
        - if read fully, or if bufPos < fillPoint, or if marked - do not fill.
        - update readerPos (total amount consumed from this CharacterReader) += bufPos
        - shift charBuf contents such that bufPos = 0; set next read offset (bufLength) -= shift amount
        - loop read the Reader until we fill charBuf. bufLength += read.
        - readFully = true when read = -1
         */
        consumed += bufPos;
        bufLength -= bufPos;
        if (bufLength > 0)
            System.arraycopy(charBuf, bufPos, charBuf, 0, bufLength);
        bufPos = 0;
        while (bufLength < BufferSize) {
            try {
                int read = reader.read(charBuf, bufLength, charBuf.length - bufLength);
                if (read == -1) {
                    readFully = true;
                    break;
                }
                bufLength += read;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        fillPoint = Math.min(bufLength, RefillPoint);

        scanBufferForNewlines(); // if enabled, we index newline positions for line number tracking
        lastIcSeq = null; // cache for last containsIgnoreCase(seq)
    }

    void mark() {
        // make sure there is enough look ahead capacity
        if (bufLength - bufPos < RewindLimit)
            fillPoint = 0;

        bufferUp();
        bufMark = bufPos;
    }

    void unmark() {
        bufMark = -1;
    }

    void rewindToMark() {
        if (bufMark == -1)
            throw new UncheckedIOException(new IOException("Mark invalid"));

        bufPos = bufMark;
        unmark();
    }

    /**
     * Gets the position currently read to in the content. Starts at 0.
     * @return current position
     */
    public int pos() {
        return consumed + bufPos;
    }

    /** Tests if the buffer has been fully read. */
    boolean readFully() {
        return readFully;
    }

    /**
     Enables or disables line number tracking. By default, will be <b>off</b>.Tracking line numbers improves the
     legibility of parser error messages, for example. Tracking should be enabled before any content is read to be of
     use.

     @param track set tracking on|off
     @since 1.14.3
     */
    public void trackNewlines(boolean track) {
        if (track && newlinePositions == null) {
            newlinePositions = new ArrayList<>(BufferSize / 80); // rough guess of likely count
            scanBufferForNewlines(); // first pass when enabled; subsequently called during bufferUp
        }
        else if (!track)
            newlinePositions = null;
    }

    /**
     Check if the tracking of newlines is enabled.
     @return the current newline tracking state
     @since 1.14.3
     */
    public boolean isTrackNewlines() {
        return newlinePositions != null;
    }

    /**
     Get the current line number (that the reader has consumed to). Starts at line #1.
     @return the current line number, or 1 if line tracking is not enabled.
     @since 1.14.3
     @see #trackNewlines(boolean)
     */
    public int lineNumber() {
        return lineNumber(pos());
    }

    int lineNumber(int pos) {
        // note that this impl needs to be called before the next buffer up or line numberoffset will be wrong. if that
        // causes issues, can remove the reset of newlinepositions during buffer, at the cost of a larger tracking array
        if (!isTrackNewlines())
            return 1;

        int i = lineNumIndex(pos);
        if (i == -1)
            return lineNumberOffset; // first line
        return i + lineNumberOffset + 1;
    }

    /**
     Get the current column number (that the reader has consumed to). Starts at column #1.
     @return the current column number
     @since 1.14.3
     @see #trackNewlines(boolean)
     */
    public int columnNumber() {
        return columnNumber(pos());
    }

    int columnNumber(int pos) {
        if (!isTrackNewlines())
            return pos + 1;

        int i = lineNumIndex(pos);
        if (i == -1)
          return pos + 1;
        return pos - newlinePositions.get(i) + 1;
    }

    /**
     Get a formatted string representing the current line and column positions. E.g. <code>5:10</code> indicating line
     number 5 and column number 10.
     @return line:col position
     @since 1.14.3
     @see #trackNewlines(boolean)
     */
    String posLineCol() {
        return lineNumber() + ":" + columnNumber();
    }

    private int lineNumIndex(int pos) {
        if (!isTrackNewlines()) return 0;
        int i = Collections.binarySearch(newlinePositions, pos);
        if (i < -1) i = Math.abs(i) - 2;
        return i;
    }

    /**
     Scans the buffer for newline position, and tracks their location in newlinePositions.
     */
    private void scanBufferForNewlines() {
        if (!isTrackNewlines())
            return;

        if (newlinePositions.size() > 0) {
            // work out the line number that we have read up to (as we have likely scanned past this point)
            int index = lineNumIndex(consumed);
            if (index == -1) index = 0; // first line
            int linePos = newlinePositions.get(index);
            lineNumberOffset += index; // the num lines we've read up to
            newlinePositions.clear();
            newlinePositions.add(linePos); // roll the last read pos to first, for cursor num after buffer
        }

        for (int i = bufPos; i < bufLength; i++) {
            if (charBuf[i] == '\n')
                newlinePositions.add(1 + consumed + i);
        }
    }

    /**
     * Tests if all the content has been read.
     * @return true if nothing left to read.
     */
    public boolean isEmpty() {
        bufferUp();
        return bufPos >= bufLength;
    }

    private boolean isEmptyNoBufferUp() {
        return bufPos >= bufLength;
    }

    /**
     * Get the char at the current position.
     * @return char
     */
    public char current() {
        bufferUp();
        return isEmptyNoBufferUp() ? EOF : charBuf[bufPos];
    }

    char consume() {
        bufferUp();
        char val = isEmptyNoBufferUp() ? EOF : charBuf[bufPos];
        bufPos++;
        return val;
    }

    /**
     Unconsume one character (bufPos--). MUST only be called directly after a consume(), and no chance of a bufferUp.
     */
    void unconsume() {
        if (bufPos < 1)
            throw new UncheckedIOException(new IOException("WTF: No buffer left to unconsume.")); // a bug if this fires, need to trace it.

        bufPos--;
    }

    /**
     * Moves the current position by one.
     */
    public void advance() {
        bufPos++;
    }

    /**
     * Returns the number of characters between the current position and the next instance of the input char
     * @param c scan target
     * @return offset between current position and next instance of target. -1 if not found.
     */
    int nextIndexOf(char c) {
        // doesn't handle scanning for surrogates
        bufferUp();
        for (int i = bufPos; i < bufLength; i++) {
            if (c == charBuf[i])
                return i - bufPos;
        }
        return -1;
    }

    /**
     * Returns the number of characters between the current position and the next instance of the input sequence
     *
     * @param seq scan target
     * @return offset between current position and next instance of target. -1 if not found.
     */
    int nextIndexOf(CharSequence seq) {
        bufferUp();
        // doesn't handle scanning for surrogates
        char startChar = seq.charAt(0);
        for (int offset = bufPos; offset < bufLength; offset++) {
            // scan to first instance of startchar:
            if (startChar != charBuf[offset])
                while(++offset < bufLength && startChar != charBuf[offset]) { /* empty */ }
            int i = offset + 1;
            int last = i + seq.length()-1;
            if (offset < bufLength && last <= bufLength) {
                for (int j = 1; i < last && seq.charAt(j) == charBuf[i]; i++, j++) { /* empty */ }
                if (i == last) // found full sequence
                    return offset - bufPos;
            }
        }
        return -1;
    }

    /**
     * Reads characters up to the specific char.
     * @param c the delimiter
     * @return the chars read
     */
    public String consumeTo(char c) {
        int offset = nextIndexOf(c);
        if (offset != -1) {
            String consumed = cacheString(charBuf, stringCache, bufPos, offset);
            bufPos += offset;
            return consumed;
        } else {
            return consumeToEnd();
        }
    }

    String consumeTo(String seq) {
        int offset = nextIndexOf(seq);
        if (offset != -1) {
            String consumed = cacheString(charBuf, stringCache, bufPos, offset);
            bufPos += offset;
            return consumed;
        } else if (bufLength - bufPos < seq.length()) {
            // nextIndexOf() did a bufferUp(), so if the buffer is shorter than the search string, we must be at EOF
            return consumeToEnd();
        } else {
            // the string we're looking for may be straddling a buffer boundary, so keep (length - 1) characters
            // unread in case they contain the beginning of the search string
            int endPos = bufLength - seq.length() + 1;
            String consumed = cacheString(charBuf, stringCache, bufPos, endPos - bufPos);
            bufPos = endPos;
            return consumed;
        }
    }

    /**
     * Read characters until the first of any delimiters is found.
     * @param chars delimiters to scan for
     * @return characters read up to the matched delimiter.
     */
    public String consumeToAny(final char... chars) {
        bufferUp();
        int pos = bufPos;
        final int start = pos;
        final int remaining = bufLength;
        final char[] val = charBuf;
        final int charLen = chars.length;
        int i;

        OUTER: while (pos < remaining) {
            for (i = 0; i < charLen; i++) {
                if (val[pos] == chars[i])
                    break OUTER;
            }
            pos++;
        }

        bufPos = pos;
        return pos > start ? cacheString(charBuf, stringCache, start, pos -start) : "";
    }

    String consumeToAnySorted(final char... chars) {
        bufferUp();
        int pos = bufPos;
        final int start = pos;
        final int remaining = bufLength;
        final char[] val = charBuf;

        while (pos < remaining) {
            if (Arrays.binarySearch(chars, val[pos]) >= 0)
                break;
            pos++;
        }
        bufPos = pos;
        return bufPos > start ? cacheString(charBuf, stringCache, start, pos -start) : "";
    }

    String consumeData() {
        // &, <, null
        //bufferUp(); // no need to bufferUp, just called consume()
        int pos = bufPos;
        final int start = pos;
        final int remaining = bufLength;
        final char[] val = charBuf;

        OUTER: while (pos < remaining) {
            switch (val[pos]) {
                case '&':
                case '<':
                case TokeniserState.nullChar:
                    break OUTER;
                default:
                    pos++;
            }
        }
        bufPos = pos;
        return pos > start ? cacheString(charBuf, stringCache, start, pos -start) : "";
    }

    String consumeAttributeQuoted(final boolean single) {
        // null, " or ', &
        //bufferUp(); // no need to bufferUp, just called consume()
        int pos = bufPos;
        final int start = pos;
        final int remaining = bufLength;
        final char[] val = charBuf;

        OUTER: while (pos < remaining) {
            switch (val[pos]) {
                case '&':
                case TokeniserState.nullChar:
                    break OUTER;
                case '\'':
                    if (single) break OUTER;
                    break;
                case '"':
                    if (!single) break OUTER;
                    break;
            }
            pos++;
        }
        bufPos = pos;
        return pos > start ? cacheString(charBuf, stringCache, start, pos -start) : "";
    }


    String consumeRawData() {
        // <, null
        //bufferUp(); // no need to bufferUp, just called consume()
        int pos = bufPos;
        final int start = pos;
        final int remaining = bufLength;
        final char[] val = charBuf;

        OUTER: while (pos < remaining) {
            switch (val[pos]) {
                case '<':
                case TokeniserState.nullChar:
                    break OUTER;
                default:
                    pos++;
            }
        }
        bufPos = pos;
        return pos > start ? cacheString(charBuf, stringCache, start, pos -start) : "";
    }

    String consumeTagName() {
        // '\t', '\n', '\r', '\f', ' ', '/', '>'
        // NOTE: out of spec; does not stop and append on nullChar but eats
        bufferUp();
        int pos = bufPos;
        final int start = pos;
        final int remaining = bufLength;
        final char[] val = charBuf;

        OUTER: while (pos < remaining) {
            switch (val[pos]) {
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                case ' ':
                case '/':
                case '>':
                    break OUTER;
            }
            pos++;
        }

        bufPos = pos;
        return pos > start ? cacheString(charBuf, stringCache, start, pos -start) : "";
    }

    String consumeToEnd() {
        bufferUp();
        String data = cacheString(charBuf, stringCache, bufPos, bufLength - bufPos);
        bufPos = bufLength;
        return data;
    }

    String consumeLetterSequence() {
        bufferUp();
        int start = bufPos;
        while (bufPos < bufLength) {
            char c = charBuf[bufPos];
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || Character.isLetter(c))
                bufPos++;
            else
                break;
        }

        return cacheString(charBuf, stringCache, start, bufPos - start);
    }

    String consumeLetterThenDigitSequence() {
        bufferUp();
        int start = bufPos;
        while (bufPos < bufLength) {
            char c = charBuf[bufPos];
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || Character.isLetter(c))
                bufPos++;
            else
                break;
        }
        while (!isEmptyNoBufferUp()) {
            char c = charBuf[bufPos];
            if (c >= '0' && c <= '9')
                bufPos++;
            else
                break;
        }

        return cacheString(charBuf, stringCache, start, bufPos - start);
    }

    String consumeHexSequence() {
        bufferUp();
        int start = bufPos;
        while (bufPos < bufLength) {
            char c = charBuf[bufPos];
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))
                bufPos++;
            else
                break;
        }
        return cacheString(charBuf, stringCache, start, bufPos - start);
    }

    String consumeDigitSequence() {
        bufferUp();
        int start = bufPos;
        while (bufPos < bufLength) {
            char c = charBuf[bufPos];
            if (c >= '0' && c <= '9')
                bufPos++;
            else
                break;
        }
        return cacheString(charBuf, stringCache, start, bufPos - start);
    }

    boolean matches(char c) {
        return !isEmpty() && charBuf[bufPos] == c;

    }

    boolean matches(String seq) {
        bufferUp();
        int scanLength = seq.length();
        if (scanLength > bufLength - bufPos)
            return false;

        for (int offset = 0; offset < scanLength; offset++)
            if (seq.charAt(offset) != charBuf[bufPos +offset])
                return false;
        return true;
    }

    boolean matchesIgnoreCase(String seq) {
        bufferUp();
        int scanLength = seq.length();
        if (scanLength > bufLength - bufPos)
            return false;

        for (int offset = 0; offset < scanLength; offset++) {
            char upScan = Character.toUpperCase(seq.charAt(offset));
            char upTarget = Character.toUpperCase(charBuf[bufPos + offset]);
            if (upScan != upTarget)
                return false;
        }
        return true;
    }

    boolean matchesAny(char... seq) {
        if (isEmpty())
            return false;

        bufferUp();
        char c = charBuf[bufPos];
        for (char seek : seq) {
            if (seek == c)
                return true;
        }
        return false;
    }

    boolean matchesAnySorted(char[] seq) {
        bufferUp();
        return !isEmpty() && Arrays.binarySearch(seq, charBuf[bufPos]) >= 0;
    }

    boolean matchesLetter() {
        if (isEmpty())
            return false;
        char c = charBuf[bufPos];
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || Character.isLetter(c);
    }

    /**
     Checks if the current pos matches an ascii alpha (A-Z a-z) per https://infra.spec.whatwg.org/#ascii-alpha
     @return if it matches or not
     */
    boolean matchesAsciiAlpha() {
        if (isEmpty())
            return false;
        char c = charBuf[bufPos];
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    boolean matchesDigit() {
        if (isEmpty())
            return false;
        char c = charBuf[bufPos];
        return (c >= '0' && c <= '9');
    }

    boolean matchConsume(String seq) {
        bufferUp();
        if (matches(seq)) {
            bufPos += seq.length();
            return true;
        } else {
            return false;
        }
    }

    boolean matchConsumeIgnoreCase(String seq) {
        if (matchesIgnoreCase(seq)) {
            bufPos += seq.length();
            return true;
        } else {
            return false;
        }
    }

    // we maintain a cache of the previously scanned sequence, and return that if applicable on repeated scans.
    // that improves the situation where there is a sequence of <p<p<p<p<p<p<p...</title> and we're bashing on the <p
    // looking for the </title>. Resets in bufferUp()
    @Nullable private String lastIcSeq; // scan cache
    private int lastIcIndex; // nearest found indexOf

    /** Used to check presence of </title>, </style> when we're in RCData and see a <xxx. Only finds consistent case. */
    boolean containsIgnoreCase(String seq) {
        if (seq.equals(lastIcSeq)) {
            if (lastIcIndex == -1) return false;
            if (lastIcIndex >= bufPos) return true;
        }
        lastIcSeq = seq;

        String loScan = seq.toLowerCase(Locale.ENGLISH);
        int lo = nextIndexOf(loScan);
        if (lo > -1) {
            lastIcIndex = bufPos + lo; return true;
        }

        String hiScan = seq.toUpperCase(Locale.ENGLISH);
        int hi = nextIndexOf(hiScan);
        boolean found = hi > -1;
        lastIcIndex = found ? bufPos + hi : -1; // we don't care about finding the nearest, just that buf contains
        return found;
    }

    @Override
    public String toString() {
        if (bufLength - bufPos < 0)
            return "";
        return new String(charBuf, bufPos, bufLength - bufPos);
    }

    /**
     * Caches short strings, as a flyweight pattern, to reduce GC load. Just for this doc, to prevent leaks.
     * <p />
     * Simplistic, and on hash collisions just falls back to creating a new string, vs a full HashMap with Entry list.
     * That saves both having to create objects as hash keys, and running through the entry list, at the expense of
     * some more duplicates.
     */
    private static String cacheString(final char[] charBuf, final String[] stringCache, final int start, final int count) {
        if (count > MaxStringCacheLen) // don't cache strings that are too big
            return new String(charBuf, start, count);
        if (count < 1)
            return "";

        // calculate hash:
        int hash = 0;
        int end = count + start;
        for (int i = start; i < end; i++) {
            hash = 31 * hash + charBuf[i];
        }

        // get from cache
        final int index = hash & StringCacheSize - 1;
        String cached = stringCache[index];

        if (cached != null && rangeEquals(charBuf, start, count, cached)) // positive hit
            return cached;
        else {
            cached = new String(charBuf, start, count);
            stringCache[index] = cached; // add or replace, assuming most recently used are most likely to recur next
        }

        return cached;
    }

    /**
     * Check if the value of the provided range equals the string.
     */
    static boolean rangeEquals(final char[] charBuf, final int start, int count, final String cached) {
        if (count == cached.length()) {
            int i = start;
            int j = 0;
            while (count-- != 0) {
                if (charBuf[i++] != cached.charAt(j++))
                    return false;
            }
            return true;
        }
        return false;
    }

    // just used for testing
    boolean rangeEquals(final int start, final int count, final String cached) {
        return rangeEquals(charBuf, start, count, cached);
    }
}
