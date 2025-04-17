package org.jsoup.parser;

import org.jsoup.internal.StringUtil;
import org.jsoup.helper.Validate;

/**
 A character reader with helpers focusing on parsing CSS selectors. Used internally by jsoup. API subject to changes.
 */

public class TokenQueue {
    private static final char Esc = '\\'; // escape char for chomp balanced.
    private static final char Hyphen_Minus = '-';
    private static final char Unicode_Null = '\u0000';
    private static final char Replacement = '\uFFFD';

    private final CharacterReader reader;

    /**
     Create a new TokenQueue.
     @param data string of data to back queue.
     */
    public TokenQueue(String data) {
        reader = new CharacterReader(data);
    }

    /**
     Is the queue empty?
     @return true if no data left in queue.
     */
    public boolean isEmpty() {
        return reader.isEmpty();
    }

    /**
     Consume one character off queue.
     @return first character on queue.
     */
    public char consume() {
        return reader.consume();
    }

    /**
     Drops the next character off the queue.
     */
    public void advance() {
        if (!isEmpty()) reader.advance();
    }

    char current() {
        return reader.current();
    }

    /**
     Internal method, no longer supported.
     @deprecated will be removed in 1.21.1.
     */
    @Deprecated public void addFirst(String seq) {
        // only left in for API compat; could not find any public uses
        // not very performant, but an edge case
        throw new UnsupportedOperationException("addFirst() not supported");
    }

    /**
     Tests if the next characters on the queue match the sequence, case-insensitively.
     @param seq String to check queue for.
     @return true if the next characters match.
     */
    public boolean matches(String seq) {
        return reader.matchesIgnoreCase(seq);
    }

    /**
     @deprecated will be removed in 1.21.1.
     */
    @Deprecated public boolean matchesAny(String... seq) {
        for (String s : seq) {
            if (matches(s))
                return true;
        }
        return false;
    }

    /**
     Tests if the next characters match any of the sequences, case-<b>sensitively</b>.
     @param seq list of chars to case-sensitively check for
     @return true of any matched, false if none did
     */
    public boolean matchesAny(char... seq) {
        return reader.matchesAny(seq);
    }

    /**
     If the queue case-insensitively matches the supplied string, consume it off the queue.
     @param seq String to search for, and if found, remove from queue.
     @return true if found and removed, false if not found.
     */
    public boolean matchChomp(String seq) {
        return reader.matchConsumeIgnoreCase(seq);
    }

    /**
     Tests if queue starts with a whitespace character.
     @return if starts with whitespace
     */
    public boolean matchesWhitespace() {
        return StringUtil.isWhitespace(reader.current());
    }

    /**
     Test if the queue matches a tag word character (letter or digit).
     @return if matches a word character
     */
    public boolean matchesWord() {
        return Character.isLetterOrDigit(reader.current());
    }

    /**
     Consumes the supplied sequence of the queue, case-insensitively. If the queue does not start with the supplied
     sequence, will throw an illegal state exception -- but you should be running match() against that condition.

     @param seq sequence to remove from head of queue.
     */
    public void consume(String seq) {
        boolean found = reader.matchConsumeIgnoreCase(seq);
        if (!found) throw new IllegalStateException("Queue did not match expected sequence");
    }

    /**
     Pulls a string off the queue, up to but exclusive of the match sequence, or to the queue running out.
     @param seq String to end on (and not include in return, but leave on queue). <b>Case-sensitive.</b>
     @return The matched data consumed from queue.
     */
    public String consumeTo(String seq) {
        return reader.consumeTo(seq);
    }

    /*
     @deprecated will be removed in 1.21.1
     */
    @Deprecated public String consumeToIgnoreCase(String seq) {
        StringBuilder sb = StringUtil.borrowBuilder();
        while (!isEmpty() && !reader.matchesIgnoreCase(seq)) {
            sb.append(consume());
        }
        return StringUtil.releaseBuilder(sb);
    }

    /**
     Consumes to the first sequence provided, or to the end of the queue. Leaves the terminator on the queue.
     @param seq any number of terminators to consume to. <b>Case-insensitive.</b>
     @return consumed string
     */
    public String consumeToAny(String... seq) {
        StringBuilder sb = StringUtil.borrowBuilder();
        OUT: while (!isEmpty()) {
            for (String s : seq) {
                if (reader.matchesIgnoreCase(s)) break OUT;
            }
            sb.append(consume());
        }
        return StringUtil.releaseBuilder(sb);
    }

    /**
     * Pulls a string off the queue (like consumeTo), and then pulls off the matched string (but does not return it).
     * <p>
     * If the queue runs out of characters before finding the seq, will return as much as it can (and queue will go
     * isEmpty() == true).
     * @param seq String to match up to, and not include in return, and to pull off queue. <b>Case-sensitive.</b>
     * @return Data matched from queue.
     * @deprecated will be removed in 1.21.1
     */
    @Deprecated public String chompTo(String seq) {
        String data = reader.consumeTo(seq);
        matchChomp(seq);
        return data;
    }

    /**
     @deprecated will be removed in 1.21.1.
     */
    @Deprecated public String chompToIgnoreCase(String seq) {
        String data = consumeToIgnoreCase(seq); // case insensitive scan
        matchChomp(seq);
        return data;
    }

    /**
     Pulls a balanced string off the queue. E.g. if queue is "(one (two) three) four", (,) will return "one (two) three",
     and leave " four" on the queue. Unbalanced openers and closers can be quoted (with ' or ") or escaped (with \).
     Those escapes will be left in the returned string, which is suitable for regexes (where we need to preserve the
     escape), but unsuitable for contains text strings; use unescape for that.

     @param open opener
     @param close closer
     @return data matched from the queue
     */
    public String chompBalanced(char open, char close) {
        StringBuilder accum = StringUtil.borrowBuilder();
        int depth = 0;
        char last = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inRegexQE = false; // regex \Q .. \E escapes from Pattern.quote()
        reader.mark(); // mark the initial position to restore if needed

        do {
            if (isEmpty()) break;
            char c = consume();
            if (last != Esc) {
                if (c == '\'' && c != open && !inDoubleQuote)
                    inSingleQuote = !inSingleQuote;
                else if (c == '"' && c != open && !inSingleQuote)
                    inDoubleQuote = !inDoubleQuote;
                if (inSingleQuote || inDoubleQuote || inRegexQE) {
                    accum.append(c);
                    last = c;
                    continue;
                }

                if (c == open) {
                    depth++;
                    if (depth > 1) accum.append(c); // don't include the outer match pair in the return
                }
                else if (c == close) {
                    depth--;
                    if (depth > 0) accum.append(c); // don't include the outer match pair in the return
                } else {
                    accum.append(c);
                }
            } else if (c == 'Q') {
                inRegexQE = true;
                accum.append(c);
            } else if (c == 'E') {
                inRegexQE = false;
                accum.append(c);
            } else {
                accum.append(c);
            }

            last = c;
        } while (depth > 0);

        String out = StringUtil.releaseBuilder(accum);
        if (depth > 0) {// ran out of queue before seeing enough )
            reader.rewindToMark(); // restore position if we don't have a balanced string
            Validate.fail("Did not find balanced marker at '" + out + "'");
        }
        return out;
    }
    
    /**
     * Unescape a \ escaped string.
     * @param in backslash escaped string
     * @return unescaped string
     */
    public static String unescape(String in) {
        if (in.indexOf(Esc) == -1) return in;

        StringBuilder out = StringUtil.borrowBuilder();
        char last = 0;
        for (char c : in.toCharArray()) {
            if (c == Esc) {
                if (last == Esc) {
                    out.append(c);
                    c = 0;
                }
            }
            else 
                out.append(c);
            last = c;
        }
        return StringUtil.releaseBuilder(out);
    }

    /*
    Given a CSS identifier (such as a tag, ID, or class), escape any CSS special characters that would otherwise not be
    valid in a selector.
     */
    public static String escapeCssIdentifier(String in) {
        StringBuilder out = StringUtil.borrowBuilder();
        TokenQueue q = new TokenQueue(in);
        while (!q.isEmpty()) {
            if (q.matchesCssIdentifier(CssIdentifierChars)) {
                out.append(q.consume());
            } else {
                out.append(Esc).append(q.consume());
            }
        }
        return StringUtil.releaseBuilder(out);
    }

    /**
     * Pulls the next run of whitespace characters of the queue.
     * @return Whether consuming whitespace or not
     */
    public boolean consumeWhitespace() {
        boolean seen = false;
        while (matchesWhitespace()) {
            advance();
            seen = true;
        }
        return seen;
    }

    /**
     * Retrieves the next run of word type (letter or digit) off the queue.
     * @return String of word characters from queue, or empty string if none.
     @deprecated will be removed in 1.21.1
     */
    @Deprecated public String consumeWord() {
        return reader.consumeMatching(Character::isLetterOrDigit);
    }

    /**
     * Consume a CSS element selector (tag name, but | instead of : for namespaces (or *| for wildcard namespace), to not conflict with :pseudo selects).
     * 
     * @return tag name
     */
    public String consumeElementSelector() {
        return consumeEscapedCssIdentifier(ElementSelectorChars);
    }
    private static final char[] ElementSelectorChars = {'*', '|', '_', '-'};

    /**
     Consume a CSS identifier (ID or class) off the queue.
     <p>Note: For backwards compatibility this method supports improperly formatted CSS identifiers, e.g. {@code 1} instead
     of {@code \31}.</p>

     @return The unescaped identifier.
     @throws IllegalArgumentException if an invalid escape sequence was found. Afterward, the state of the TokenQueue
     is undefined.
     @see <a href="https://www.w3.org/TR/css-syntax-3/#consume-name">CSS Syntax Module Level 3, Consume an ident sequence</a>
     @see <a href="https://www.w3.org/TR/css-syntax-3/#typedef-ident-token">CSS Syntax Module Level 3, ident-token</a>
     */
    public String consumeCssIdentifier() {
        if (isEmpty()) throw new IllegalArgumentException("CSS identifier expected, but end of input found");

        // Fast path for CSS identifiers that don't contain escape sequences.
        String identifier = reader.consumeMatching(TokenQueue::isIdent);
        char c = current();
        if (c != Esc && c != Unicode_Null) {
            // If we didn't end on an Esc or a Null, we consumed the whole identifier
            return identifier;
        }

        // An escape sequence was found. Use a StringBuilder to store the decoded CSS identifier.
        StringBuilder out = StringUtil.borrowBuilder();
        if (!identifier.isEmpty()) {
            // Copy the CSS identifier up to the first escape sequence.
            out.append(identifier);
        }

        while (!isEmpty()) {
            c = current();
            if (isIdent(c)) {
                out.append(consume());
            } else if (c == Unicode_Null) {
                // https://www.w3.org/TR/css-syntax-3/#input-preprocessing
                advance();
                out.append(Replacement);
            } else if (c == Esc) {
                advance();
                if (!isEmpty() && isNewline(current())) {
                    // Not a valid escape sequence. This is treated as the end of the CSS identifier.
                    reader.unconsume();
                    break;
                } else {
                    consumeCssEscapeSequenceInto(out);
                }
            } else {
                break;
            }
        }
        return StringUtil.releaseBuilder(out);
    }

    private void consumeCssEscapeSequenceInto(StringBuilder out) {
        if (isEmpty()) {
            out.append(Replacement);
            return;
        }

        char firstEscaped = consume();
        if (!CharacterReader.isHexDigit(firstEscaped)) {
            out.append(firstEscaped);
        } else {
            reader.unconsume(); // put back the first hex digit
            String hexString = reader.consumeMatching(CharacterReader::isHexDigit, 6); // consume up to 6 hex digits
            int codePoint;
            try {
                codePoint = Integer.parseInt(hexString, 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid escape sequence: " + hexString, e);
            }
            if (isValidCodePoint(codePoint)) {
                out.appendCodePoint(codePoint);
            } else {
                out.append(Replacement);
            }

            if (!isEmpty()) {
                char c = current();
                if (c == '\r') {
                    // Since there's currently no input preprocessing, check for CRLF here.
                    // https://www.w3.org/TR/css-syntax-3/#input-preprocessing
                    advance();
                    if (!isEmpty() && current() == '\n') advance();
                } else if (c == ' ' || c == '\t' || isNewline(c)) {
                    advance();
                }
            }
        }
    }

    // statics below specifically for CSS identifiers:

    // https://www.w3.org/TR/css-syntax-3/#non-ascii-code-point
    private static boolean isNonAscii(char c) {
        return c >= '\u0080';
    }

    // https://www.w3.org/TR/css-syntax-3/#ident-start-code-point
    private static boolean isIdentStart(char c) {
        return c == '_' || CharacterReader.isAsciiLetter(c) || isNonAscii(c);
    }

    // https://www.w3.org/TR/css-syntax-3/#ident-code-point
    private static boolean isIdent(char c) {
        return c == Hyphen_Minus || CharacterReader.isDigit(c) || isIdentStart(c);
    }

    // https://www.w3.org/TR/css-syntax-3/#newline
    // Note: currently there's no preprocessing happening.
    private static boolean isNewline(char c) {
        return c == '\n' || c == '\r' || c == '\f';
    }

    // https://www.w3.org/TR/css-syntax-3/#consume-an-escaped-code-point
    private static boolean isValidCodePoint(int codePoint) {
        return codePoint != 0 && Character.isValidCodePoint(codePoint) && !Character.isSurrogate((char) codePoint);
    }

    private static final char[] CssIdentifierChars = {'-', '_'};

    private String consumeEscapedCssIdentifier(char... matches) {
        StringBuilder sb = StringUtil.borrowBuilder();
        while (!isEmpty()) {
            char c = current();
            if (c == Esc) {
                advance();
                if (!isEmpty()) sb.append(consume());
                else break;
            } else if (matchesCssIdentifier(matches)) {
                sb.append(c);
                advance();
            } else {
                break;
            }
        }
        return StringUtil.releaseBuilder(sb);
    }

    private boolean matchesCssIdentifier(char... matches) {
        return matchesWord() || reader.matchesAny(matches);
    }

    /**
     Consume and return whatever is left on the queue.
     @return remainder of queue.
     */
    public String remainder() {
        return reader.consumeToEnd();
    }

    @Override
    public String toString() {
        return reader.toString();
    }
}
