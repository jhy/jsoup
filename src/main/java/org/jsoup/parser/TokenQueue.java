package org.jsoup.parser;

import org.apache.commons.lang.Validate;

/**
 * A character queue with parsing helpers.
 *
 * @author Jonathan Hedley
 */
public class TokenQueue {
    private StringBuilder queue;
    private StringBuilder lcQueue; // lower-cased clone of the queue, for faster matching 
    private int pos = 0;
    
    private static final Character ESC = '\\'; // escape char for chomp balanced.

    /**
     Create a new TokenQueue.
     @param data string of data to back queue.
     */
    public TokenQueue(String data) {
        Validate.notNull(data);

        queue = new StringBuilder(data);
        lcQueue = new StringBuilder(data.toLowerCase());
    }

    /**
     * Is the queue empty?
     * @return true if no data left in queue.
     */
    public boolean isEmpty() {
        return remainingLength() == 0;
    }
    
    private int remainingLength() {
        return queue.length() - pos;
    }

    /**
     * Retrieves but does not remove the first characater from the queue.
     * @return First character, or null if empty.
     */
    public Character peek() {
        return isEmpty() ? null : queue.charAt(pos);
    }

    /**
     Add a character to the start of the queue (will be the next character retrieved).
     @param c character to add
     */
    public void addFirst(Character c) {
        queue.insert(pos, c);
        lcQueue.insert(pos, Character.toLowerCase(c));
    }

    /**
     Add a string to the start of the queue.
     @param seq string to add.
     */
    public void addFirst(String seq) {
        queue.insert(pos, seq);
        lcQueue.insert(pos, seq.toLowerCase());
    }

    /**
     * Tests if the next characters on the queue match the sequence. Case insensitive.
     * @param seq String to check queue for.
     * @return true if the next characters match.
     */
    public boolean matches(String seq) {
        int len = seq.length();
        if (len > remainingLength())
            return false;
        String check = lcQueue.substring(pos, pos+len);
        return seq.toLowerCase().equals(check);
    }
    

    /**
     Tests if the next characters match any of the sequences.
     @param seq
     @return
     */
    public boolean matchesAny(String... seq) {
        for (String s : seq) {
            if (matches(s))
                return true;
        }
        return false;
    }

    /**
     * Tests if the queue matches the sequence (as with match), and if they do, removes the matched string from the
     * queue.
     * @param seq String to search for, and if found, remove from queue.
     * @return true if found and removed, false if not found.
     */
    public boolean matchChomp(String seq) {
        if (matches(seq)) {
            consume(seq);
            return true;
        } else {
            return false;
        }
    }

    /**
     Tests if queue starts with a whitespace character.
     @return if starts with whitespace
     */
    public boolean matchesWhitespace() {
        return !isEmpty() && Character.isWhitespace(peek());
    }

    /**
     Test if the queue matches a word character (letter or digit).
     @return if matches a word character
     */
    public boolean matchesWord() {
        return !isEmpty() && Character.isLetterOrDigit(peek());
    }

    /**
     * Consume one character off queue.
     * @return first character on queue.
     */
    public Character consume() {
        Character c= queue.charAt(pos);
        pos++;
        return c;
    }

    /**
     * Consumes the supplied sequence of the queue. If the queue does not start with the supplied sequence, will
     * throw an illegal state exception -- but you should be running match() against that condition.
     <p>
     Case insensitive.
     * @param seq sequence to remove from head of queue.
     */
    public void consume(String seq) {
        if (!matches(seq))
            throw new IllegalStateException("Queue did not match expected sequence");
        int len = seq.length();
        if (len > remainingLength())
            throw new IllegalStateException("Queue not long enough to consume sequence");
        
        pos += len;
    }

    /**
     * Pulls a string off the queue, up to but exclusive of the match sequence, or to the queue running out.
     * @param seq String to end on (and not include in return, but leave on queue)
     * @return The matched data consumed from queue.
     */
    public String consumeTo(String seq) {
        int offset = lcQueue.indexOf(seq.toLowerCase(), pos);
        if (offset != -1) {
            String consumed = queue.substring(pos, offset);
            pos += consumed.length();
            return consumed;
        } else {
            return remainder();
        }
    }

    /**
     Consumes to the first sequence provided, or to the end of the queue. Leaves the terminator on the queue.
     @param seq any number of terminators to consume to
     @return consumed string
     */
    public String consumeToAny(String... seq) {
        StringBuilder accum = new StringBuilder();
        while (!isEmpty() && !matchesAny(seq))
            accum.append(consume());

        return accum.toString();
    }

    /**
     * Pulls a string off the queue (like consumeTo), and then pulls off the matched string (but does not return it).
     * <p>
     * If the queue runs out of characters before finding the seq, will return as much as it can (and queue will go
     * isEmpty() == true).
     * @param seq String to match up to, and not include in return, and to pull off queue
     * @return Data matched from queue.
     */
    public String chompTo(String seq) {
        String data = consumeTo(seq);
        matchChomp(seq);
        return data;
    }

    /**
     * Pulls a balanced string off the queue. E.g. if queue is "(one (two) three) four", (,) will return "one (two) three",
     * and leave " four" on the queue. Unbalanced openers and closers can be escaped (with \). Those escapes will be left
     * in the returned string, which is suitable for regexes (where we need to preserve the escape), but unsuitable for
     * contains text strings; use unescape for that.
     * @param open opener
     * @param close closer
     * @return data matched from the queue
     */
    public String chompBalanced(Character open, Character close) {
        StringBuilder accum = new StringBuilder();
        int depth = 0;
        Character last = null;

        do {
            if (isEmpty()) break;
            Character c = consume();
            if (last == null || !last.equals(ESC)) {
                if (c.equals(open))
                    depth++;
                else if (c.equals(close))
                    depth--;
            }

            if (depth > 0 && last != null)
                accum.append(c); // don't include the outer match pair in the return
            last = c;
        } while (depth > 0);
        return accum.toString();
    }
    
    /**
     * Unescaped a \ escaped string.
     * @param in backslash escaped string
     * @return unescaped string
     */
    public static String unescape(String in) {
        StringBuilder out = new StringBuilder();
        Character last = null;
        for (Character c : in.toCharArray()) {
            if (c.equals(ESC)) {
                if (last != null && last.equals(ESC))
                    out.append(c);
            }
            else 
                out.append(c);
            last = c;
        }
        return out.toString();
    }

    /**
     * Pulls the next run of whitespace characters of the queue.
     */
    public boolean consumeWhitespace() {
        boolean seen = false;
        while (matchesWhitespace()) {
            consume();
            seen = true;
        }
        return seen;
    }

    /**
     * Retrieves the next run of word type (letter or digit) off the queue.
     * @return String of word characters from queue, or empty string if none.
     */
    public String consumeWord() {
        StringBuilder wordAccum = new StringBuilder();
        while (matchesWord()) {
            wordAccum.append(consume());
        }
        return wordAccum.toString();
    }

    /**
     Consume a CSS identifier (ID or class) off the queue (letter, digit, -, _)
     http://www.w3.org/TR/CSS2/syndata.html#value-def-identifier
     @return identifier
     */
    public String consumeCssIdentifier() {
        StringBuilder accum = new StringBuilder();
        Character c = peek();
        while (!isEmpty() && (Character.isLetterOrDigit(c) || c.equals('-') || c.equals('_'))) {
            accum.append(consume());
            c = peek();
        }
        return accum.toString();
    }

    /**
     Consume an attribute key off the queue (letter, digit, -, _, :")
     @return attribute key
     */
    public String consumeAttributeKey() {
        StringBuilder accum = new StringBuilder();
        while (!isEmpty() && (matchesWord() || matchesAny("-", "_", ":"))) {
            accum.append(consume());
        }
        return accum.toString();
    }

    /**
     Consume and return whatever is left on the queue.
     @return remained of queue.
     */
    public String remainder() {
        StringBuilder accum = new StringBuilder();
        while (!isEmpty()) {
            accum.append(consume());
        }
        return accum.toString();
    }
    
    public String toString() {
        return queue.toString();
    }
}
