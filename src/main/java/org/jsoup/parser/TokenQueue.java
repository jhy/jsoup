package org.jsoup.parser;

import org.apache.commons.lang.Validate;

import java.util.LinkedList;
import java.util.List;

/**
 * A character queue with parsing helpers.
 *
 * @author Jonathan Hedley
 */
class TokenQueue {
    private LinkedList<Character> queue;

    TokenQueue(String data) {
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
    boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Retrieves but does not remove the first characater from the queue.
     * @return First character, or null if empty.
     */
    Character peek() {
        return queue.peek();
    }

    /**
     * Tests if the next characters on the queue match the sequence.
     * @param seq String to check queue for.
     * @return true if the next characters match.
     */
    boolean matches(String seq) {
        int len = seq.length();
        if (len > queue.size())
            return false;
        List<Character> chars = queue.subList(0, len);
        char[] seqChars = seq.toCharArray();
        for (int i = 0; i < len; i++) {
            if (!chars.get(i).equals(seqChars[i]))
                return false;
        }
        return true;
    }

    /**
     * Tests if the queue matches the sequence (as with match), and if they do, removes the matched string from the
     * queue.
     * @param seq String to search for, and if found, remove from queue.
     * @return true if found and removed, false if not found.
     */
    boolean matchChomp(String seq) {
        if (matches(seq)) {
            consume(seq);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Consume one character off queue.
     * @return first character on queue.
     */
    Character consume() {
        return queue.removeFirst();
    }

    /**
     * Consumes the supplied sequence of the queue. If the queue does not start with the supplied sequence, will
     * throw an illegal state exception -- but you should be running match() against that condition.
     * @param seq sequence to remove from head of queue.
     */
    void consume(String seq) {
        int len = seq.length();
        if (len > queue.size())
            throw new IllegalStateException("Queue not long enough to consume sequence");
        char[] seqChars = seq.toCharArray();
        for (int i = 0; i < len; i++) {
            Character qChar = consume();
            if (!qChar.equals(seqChars[i]))
                throw new IllegalStateException("Queue did not match expected sequence");
        }
    }

    /**
     * Pulls a string off the queue, up to but exclusive of the match sequence, or to the queue running out.
     * @param seq String to end on (and not include in return, but leave on queue)
     * @return The matched data consumed from queue.
     */
    String consumeTo(String seq) {
        StringBuilder accum = new StringBuilder();
        while (!queue.isEmpty() && !matches(seq))
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
    String chompTo(String seq) {
        String data = consumeTo(seq);
        matchChomp(seq);
        return data;
    }

    /**
     * Pulls the next run of whitespace characters of the queue.
     */
    void consumeWhitespace() {
        while (!queue.isEmpty() && Character.isWhitespace(queue.peekFirst())) {
            consume();
        }
    }

    /**
     * Retrieves the next run of word type (letter or digit) off the queue.
     * @return String of word characters from queue, or empty string if none.
     */
    String consumeWord() {
        StringBuilder wordAccum = new StringBuilder();
        while (!queue.isEmpty() && Character.isLetterOrDigit(queue.peekFirst())) {
            wordAccum.append(queue.removeFirst());
        }
        return wordAccum.toString();
    }
}
