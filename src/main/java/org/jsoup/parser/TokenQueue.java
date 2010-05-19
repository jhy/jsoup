package org.jsoup.parser;

import org.apache.commons.lang.Validate;

import java.util.LinkedList;
import java.util.List;

/**
 * A character queue with parsing helpers.
 *
 * @author Jonathan Hedley
 */
public class TokenQueue {
    private LinkedList<Character> queue;

    /**
     Create a new TokenQueue.
     @param data string of data to back queue.
     */
    public TokenQueue(String data) {
        Validate.notNull(data);

        queue = new LinkedList<Character>();
        char[] chars = data.toCharArray();
        for (char c : chars) {
            queue.add(c);
        }
    }

    /**
     * Is the queue empty?
     * @return true if no data left in queue.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Retrieves but does not remove the first characater from the queue.
     * @return First character, or null if empty.
     */
    public Character peek() {
        return queue.peek();
    }

    /**
     Add a character to the start of the queue (will be the next character retrieved).
     @param c character to add
     */
    public void addFirst(Character c) {
        queue.addFirst(c);
    }

    /**
     Add a string to the start of the queue.
     @param seq string to add.
     */
    public void addFirst(String seq) {
        char[] chars = seq.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            addFirst(chars[i]);
        }
    }

    /**
     * Tests if the next characters on the queue match the sequence. Case insensitive.
     * @param seq String to check queue for.
     * @return true if the next characters match.
     */
    public boolean matches(String seq) {
        int len = seq.length();
        if (len > queue.size())
            return false;
        List<Character> chars = queue.subList(0, len);
        char[] seqChars = seq.toCharArray();
        for (int i = 0; i < len; i++) {
            Character found = Character.toLowerCase(chars.get(i));
            Character check = Character.toLowerCase(seqChars[i]);
            if (!found.equals(check))
                return false;
        }
        return true;
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
        return !queue.isEmpty() && Character.isWhitespace(queue.peek());
    }

    /**
     Test if the queue matches a word character (letter or digit).
     @return if matches a word character
     */
    public boolean matchesWord() {
        return !queue.isEmpty() && Character.isLetterOrDigit(queue.peek());
    }

    /**
     * Consume one character off queue.
     * @return first character on queue.
     */
    public Character consume() {
        return queue.removeFirst();
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
        if (len > queue.size())
            throw new IllegalStateException("Queue not long enough to consume sequence");
        for (int i = 0; i < len; i++) {
            consume();
        }
    }

    /**
     * Pulls a string off the queue, up to but exclusive of the match sequence, or to the queue running out.
     * @param seq String to end on (and not include in return, but leave on queue)
     * @return The matched data consumed from queue.
     */
    public String consumeTo(String seq) {
        return consumeToAny(seq);
    }

    /**
     Consumes to the first sequence provided, or to the end of the queue. Leaves the terminator on the queue.
     @param seq any number of terminators to consume to
     @return consumed string
     */
    public String consumeToAny(String... seq) {
        StringBuilder accum = new StringBuilder();
        while (!queue.isEmpty() && !matchesAny(seq))
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
     * Pulls the next run of whitespace characters of the queue.
     */
    public boolean consumeWhitespace() {
        boolean seen = false;
        while (!queue.isEmpty() && Character.isWhitespace(queue.peek())) {
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
        while (!queue.isEmpty() && Character.isLetterOrDigit(queue.peek())) {
            wordAccum.append(queue.removeFirst());
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
        Character c = queue.peek();
        while (!queue.isEmpty() && (Character.isLetterOrDigit(c) || c.equals('-') || c.equals('_'))) {
            accum.append(queue.removeFirst());
            c = queue.peek();
        }
        return accum.toString();
    }

    /**
     Consume an attribute key off the queue (letter, digit, -, _, :")
     @return attribute key
     */
    public String consumeAttributeKey() {
        StringBuilder accum = new StringBuilder();
        while (!queue.isEmpty() && (Character.isLetterOrDigit(queue.peek()) || matchesAny("-", "_", ":"))) {
            accum.append(queue.removeFirst());
        }
        return accum.toString();
    }

    /**
     Consume and return whatever is left on the queue.
     @return remained of queue.
     */
    public String remainder() {
        StringBuilder accum = new StringBuilder();
        while (!queue.isEmpty()) {
            accum.append(consume());
        }
        return accum.toString();
    }
    
    public String toString() {
        return queue.toString();
    }
}
