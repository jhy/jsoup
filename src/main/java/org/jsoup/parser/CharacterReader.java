package org.jsoup.parser;

import org.jsoup.UncheckedIOException;
import org.jsoup.helper.Validate;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Locale;

/**
 CharacterReader consumes tokens off a string. Used internally by jsoup. API subject to changes.
 */
public final class CharacterReader {
    static final char EOF = (char) -1;
    private static final int maxStringCacheLen = 12;
    static final int maxBufferLen = 1024 * 32; // visible for testing

    private final char[] charBuf;
    private final Reader reader;
    private int bufLength;
    private int bufPos;
    private int readerPos;
    private int bufMark = -1;
    private final String[] stringCache = new String[512]; // holds reused strings in this doc, to lessen garbage
    private int consumeStartPos = -1;
    private StringBuilder consumptionMemory;

    public CharacterReader(Reader input, int sz) {
        Validate.notNull(input);
        Validate.isTrue(input.markSupported());
        reader = input;
        charBuf = new char[sz > maxBufferLen ? maxBufferLen : sz];
        bufferUp();

        if (isBinary()) {
            throw new UncheckedIOException("Input is binary and unsupported");
        }
    }

    public CharacterReader(Reader input) {
        this(input, maxBufferLen);
    }

    public CharacterReader(String input) {
        this(new StringReader(input), input.length());
    }
    
    private void startConsume() {
        consumeStartPos = bufPos;
    }
    
    private String endConsume() {
        String result;
        if (consumptionMemory == null) {
            // simple case, didn't cross buffer boundary
            result = cacheString(charBuf, stringCache, consumeStartPos, bufPos - consumeStartPos);
        } else {
            consumptionMemory.append(charBuf, consumeStartPos, bufPos - consumeStartPos);
            result = consumptionMemory.toString();
            consumptionMemory = null;
        }
        consumeStartPos = -1;
        return result;
    }

    private void bufferUp() {
        final int pos = bufPos;
        if (pos != bufLength)   // pos > bufLength is possible after consuming an EOF, but there's no point in buffering up in that case
            return;

        if (consumeStartPos >= 0) {
            if (consumptionMemory == null) {
                consumptionMemory = new StringBuilder();
            }
            consumptionMemory.append(charBuf, consumeStartPos, pos - consumeStartPos);
        }
        // Make sure to keep the marked characters. We also need to keep the last byte of the old buffer around to make sure unconsume() works.
        int charsToKeep = 1;
        if (bufMark >= 0) {
            charsToKeep = Math.max(charsToKeep, bufPos - bufMark);
        }
        charsToKeep = Math.min(charsToKeep, bufLength);
        int copyStart = bufPos - charsToKeep;
        System.arraycopy(charBuf, copyStart, charBuf, 0, charsToKeep);
        bufPos = charsToKeep;
        bufLength = charsToKeep;
        if (bufMark >= 0) {
            bufMark -= copyStart;
        }
        try {
            final int read = reader.read(charBuf, bufPos, charBuf.length - bufPos);
            if (read != -1) {
                bufLength += read;
                readerPos += read;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (consumeStartPos >= 0) {
            consumeStartPos = bufPos;
        }
    }

    /**
     * Gets the current cursor position in the content.
     * @return current position
     */
    public int pos() {
        return readerPos - bufLength + bufPos;
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

    void unconsume() {
        if (bufPos < 1)
            throw new UncheckedIOException(new IOException("No buffer left to unconsume"));

        bufPos--;
    }

    /**
     * Moves the current position by one.
     */
    public void advance() {
        bufPos++;
    }

    void mark() {
        bufferUp();
        bufMark = bufPos;
    }

    void rewindToMark() {
        if (bufMark == -1)
            throw new UncheckedIOException(new IOException("Mark invalid"));

        bufPos = bufMark;
        bufMark = -1;
    }
    
    void releaseMark() {
        bufMark = -1;
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
        startConsume();
        int offset = bufPos;
        while (!isEmpty()) {
            offset = nextIndexOf(c);
            if (offset == -1) {
                bufPos = bufLength;
            } else {
                bufPos += offset;
                break;
            }
        }
        return endConsume();
    }

    String consumeTo(String seq) {
        startConsume();
        int offset = bufPos;
        while (!isEmpty()) {
            offset = nextIndexOf(seq);
            if (offset == -1) {
                bufPos = bufLength;
            } else {
                bufPos += offset;
                break;
            }
        }
        return endConsume();
    }

    /**
     * Read characters until the first of any delimiters is found.
     * @param chars delimiters to scan for
     * @return characters read up to the matched delimiter.
     */
    public String consumeToAny(final char... chars) {
        final int charLen = chars.length;
        final int remaining = bufLength;
        final char[] val = charBuf;
        
        startConsume();
        
        while (!isEmpty()) {
            for (int pos = bufPos; pos < remaining; pos++) {
                for (int i = 0; i < charLen; i++) {
                    if (val[pos] == chars[i]) {
                        bufPos = pos;
                        return endConsume();
                    }
                }
            }
            bufPos = remaining;
        }
        // reached the end without finding any match
        return endConsume();
    }

    String consumeToAnySorted(final char... chars) {
        final int remaining = bufLength;
        final char[] val = charBuf;
        
        startConsume();
        
        while (!isEmpty()) {
            for (int pos = bufPos; pos < remaining; pos++) {
                if (Arrays.binarySearch(chars, val[pos]) >= 0) {
                    bufPos = pos;
                    return endConsume();
                }
            }
            bufPos = remaining;
        }
        // reached the end without finding any match
        return endConsume();
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

    String consumeTagName() {
        // '\t', '\n', '\r', '\f', ' ', '/', '>', nullChar
        // NOTE: out of spec, added '<' to fix common author bugs
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
                case '<':
                case TokeniserState.nullChar:
                    break OUTER;
            }
            pos++;
        }

        bufPos = pos;
        return pos > start ? cacheString(charBuf, stringCache, start, pos -start) : "";
    }

    String consumeToEnd() {
        startConsume();
        while (!isEmpty()) {
            bufPos = bufLength;
        }
        return endConsume();
    }

    String consumeLetterSequence() {
        startConsume();
        
        while (!isEmptyNoBufferUp() || !isEmpty()) {
            char c = charBuf[bufPos];
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || Character.isLetter(c))
                bufPos++;
            else
                break;
        }
        
        return endConsume();
    }

    String consumeLetterThenDigitSequence() {
        startConsume();
        
        while (!isEmptyNoBufferUp() || !isEmpty()) {
            char c = charBuf[bufPos];
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || Character.isLetter(c))
                bufPos++;
            else
                break;
        }
        while (!isEmptyNoBufferUp() || !isEmpty()) {
            char c = charBuf[bufPos];
            if (c >= '0' && c <= '9')
                bufPos++;
            else
                break;
        }
        return endConsume();
    }

    String consumeHexSequence() {
        startConsume();
        while (!isEmptyNoBufferUp() || !isEmpty()) {
            char c = charBuf[bufPos];
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))
                bufPos++;
            else
                break;
        }
        return endConsume();
    }

    String consumeDigitSequence() {
        startConsume();
        while (!isEmptyNoBufferUp() || !isEmpty()) {
            char c = charBuf[bufPos];
            if (c >= '0' && c <= '9')
                bufPos++;
            else
                break;
        }
        return endConsume();
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

    boolean containsIgnoreCase(String seq) {
        // used to check presence of </title>, </style>. only finds consistent case.
        String loScan = seq.toLowerCase(Locale.ENGLISH);
        String hiScan = seq.toUpperCase(Locale.ENGLISH);
        return (nextIndexOf(loScan) > -1) || (nextIndexOf(hiScan) > -1);
    }

    private static final int numNullsConsideredBinary = 10; // conservative

    /**
     *  Heuristic to determine if the current buffer looks like binary content. Reader will already hopefully be
     *  decoded correctly, so a bunch of NULLs indicates a binary file
     */
    boolean isBinary() {
        int nullsSeen = 0;

        for (int i = bufPos; i < bufLength; i++) {
            if (charBuf[i] == '\0')
                nullsSeen++;
        }

        return nullsSeen >= numNullsConsideredBinary;
    }

    @Override
    public String toString() {
        return new String(charBuf, bufPos, bufLength - bufPos);
    }

    /**
     * Caches short strings, as a flywheel pattern, to reduce GC load. Just for this doc, to prevent leaks.
     * <p />
     * Simplistic, and on hash collisions just falls back to creating a new string, vs a full HashMap with Entry list.
     * That saves both having to create objects as hash keys, and running through the entry list, at the expense of
     * some more duplicates.
     */
    private static String cacheString(final char[] charBuf, final String[] stringCache, final int start, final int count) {
        // limit (no cache):
        if (count > maxStringCacheLen)
            return new String(charBuf, start, count);
        if (count < 1)
            return "";

        // calculate hash:
        int hash = 0;
        int offset = start;
        for (int i = 0; i < count; i++) {
            hash = 31 * hash + charBuf[offset++];
        }

        // get from cache
        final int index = hash & stringCache.length - 1;
        String cached = stringCache[index];

        if (cached == null) { // miss, add
            cached = new String(charBuf, start, count);
            stringCache[index] = cached;
        } else { // hashcode hit, check equality
            if (rangeEquals(charBuf, start, count, cached)) { // hit
                return cached;
            } else { // hashcode conflict
                cached = new String(charBuf, start, count);
                stringCache[index] = cached; // update the cache, as recently used strings are more likely to show up again
            }
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
