package org.jsoup.parser;

import org.jsoup.helper.Validate;

/**
 CharacterReader cosumes tokens off a string. To replace the old TokenQueue.
 */
class CharacterReader {
    static final char EOF = (char) -1;

    private final String input;
    private final int length;
    private int pos = 0;
    private int mark = 0;

    CharacterReader(String input) {
        Validate.notNull(input);
        input = input.replaceAll("\r\n?", "\n"); // normalise carriage returns to newlines

        this.input = input;
        this.length = input.length();
    }

    int pos() {
        return pos;
    }

    boolean isEmpty() {
        return pos >= length;
    }

    char current() {
        return isEmpty() ? EOF : input.charAt(pos);
    }

    char consume() {
        char val = isEmpty() ? EOF : input.charAt(pos);
        pos++;
        return val;
    }

    void unconsume() {
        pos--;
    }

    void advance() {
        pos++;
    }

    void mark() {
        mark = pos;
    }

    void rewindToMark() {
        pos = mark;
    }

    String consumeAsString() {
        return input.substring(pos, pos++);
    }

    String consumeTo(char c) {
        int offset = input.indexOf(c, pos);
        if (offset != -1) {
            String consumed = input.substring(pos, offset);
            pos += consumed.length();
            return consumed;
        } else {
            return consumeToEnd();
        }
    }

    String consumeTo(String seq) {
        int offset = input.indexOf(seq, pos);
        if (offset != -1) {
            String consumed = input.substring(pos, offset);
            pos += consumed.length();
            return consumed;
        } else {
            return consumeToEnd();
        }
    }

    String consumeToAny(char... seq) {
        int start = pos;

        OUTER: while (!isEmpty()) {
            char c = input.charAt(pos);
            for (char seek : seq) {
                if (seek == c)
                    break OUTER;
            }
            pos++;
        }

        return pos > start ? input.substring(start, pos) : "";
    }

    String consumeToEnd() {
        String data = input.substring(pos, input.length());
        pos = input.length();
        return data;
    }

    String consumeLetterSequence() {
        int start = pos;
        while (!isEmpty()) {
            char c = input.charAt(pos);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))
                pos++;
            else
                break;
        }

        return input.substring(start, pos);
    }

    String consumeHexSequence() {
        int start = pos;
        while (!isEmpty()) {
            char c = input.charAt(pos);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))
                pos++;
            else
                break;
        }
        return input.substring(start, pos);
    }

    String consumeDigitSequence() {
        int start = pos;
        while (!isEmpty()) {
            char c = input.charAt(pos);
            if (c >= '0' && c <= '9')
                pos++;
            else
                break;
        }
        return input.substring(start, pos);
    }

    boolean matches(char c) {
        return !isEmpty() && input.charAt(pos) == c;

    }

    boolean matches(String seq) {
        return input.startsWith(seq, pos);
    }

    boolean matchesIgnoreCase(String seq) {
        return input.regionMatches(true, pos, seq, 0, seq.length());
    }

    boolean matchesAny(char... seq) {
        if (isEmpty())
            return false;

        char c = input.charAt(pos);
        for (char seek : seq) {
            if (seek == c)
                return true;
        }
        return false;
    }

    boolean matchesLetter() {
        if (isEmpty())
            return false;
        char c = input.charAt(pos);
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    boolean matchesDigit() {
        if (isEmpty())
            return false;
        char c = input.charAt(pos);
        return (c >= '0' && c <= '9');
    }

    boolean matchConsume(String seq) {
        if (matches(seq)) {
            pos += seq.length();
            return true;
        } else {
            return false;
        }
    }

    boolean matchConsumeIgnoreCase(String seq) {
        if (matchesIgnoreCase(seq)) {
            pos += seq.length();
            return true;
        } else {
            return false;
        }
    }

    boolean containsIgnoreCase(String seq) {
        // used to check presence of </title>, </style>. only finds consistent case.
        String loScan = seq.toLowerCase();
        String hiScan = seq.toUpperCase();
        return (input.indexOf(loScan, pos) > -1) || (input.indexOf(hiScan, pos) > -1);
    }

    @Override
    public String toString() {
        return input.substring(pos);
    }
}
