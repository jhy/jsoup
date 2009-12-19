package org.jsoup.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 Splits a given input into a stream of {@link Token Tokens}. (Internal use only).

 @author Jonathan Hedley, jonathan@hedley.net */
class TokenStream implements Iterator<Token> {
    private static final int BUFFER_SIZE = 5 * 1024;
    private static final char LT = "<".charAt(0);
    private static final char GT = ">".charAt(0);
    private static final char NL = "\n".charAt(0);

    private final Reader in;
    private final CharBuffer buffer;
    private final StringBuilder accum;
    private final boolean autoCloseReader;
    private final Position pos;

    private TokenStream(Reader reader, boolean autoCloseReader) {
        this.in = reader;
        this.autoCloseReader = autoCloseReader;
        buffer = CharBuffer.allocate(BUFFER_SIZE);
        accum = new StringBuilder(BUFFER_SIZE / 2);
        pos = new Position();

        fillBuffer();
    }

    /**
     TokenStream factory: extract tokens from the supplied Reader. Don't forget to close the Reader once you're done.
     @param reader input reader
     @return TokenStream of HTML.
     */
    public static TokenStream create(Reader reader) {
        return new TokenStream(reader, false);
    }

    /**
     TokenStream factory method: extract tokens from a String.
     @param data String of HTML data.
     @return TokenStream of HTML.
     */
    public static TokenStream create(String data) {
        StringReader reader = new StringReader(data);
        return new TokenStream(reader, true);
    }

    /**
     Test if there are any more tokens to be retrieved.
     @return true if there are tokens remaining, false if all have been read.
     */
    public boolean hasNext() {
        boolean hasNext = (buffer.hasRemaining() || (fillBuffer() > -1));

        if (!hasNext && autoCloseReader)
            try {
                in.close();
            } catch (IOException e) {
                throw new ParserRuntimeException("IO exception whilst auto-closing reader", e, pos);
            }
        return hasNext;
    }

    /**
     Retrieve the next Token. Make sure there are some left using hasNext().
     @return next token from stream.
     */
    public Token next() {
        Position curPos = pos.clone();

        String tokenData = accumulate();
        return new Token(tokenData, curPos);
    }

    List<Token> asList() {
        List<Token> tokens = new ArrayList<Token>();
        while (hasNext()) {
            tokens.add(next());
        }
        return tokens;
    }

    private String accumulate() {
        while (hasNext()) {
            buffer.mark();
            char c = buffer.get();

            if (c == LT && accum.length() > 0) {
                // leave on stack for next accumulate
                buffer.reset();
                return captureAccum();
            } else {
                accum.append(c);
                pos.incOffset();
                pos.incColNum();

                if (c == GT) {
                    return captureAccum();
                } else if (c == NL) {
                    pos.incLineNum();
                }
            }
        }
        return captureAccum();
    }

    private int fillBuffer() {
        buffer.clear();
        try {
            int charsRead = in.read(buffer);
            buffer.flip();
            return charsRead;
        } catch (IOException e) {
            throw new ParserRuntimeException("IO exception whilst reading", e, pos);
        }
    }

    private String captureAccum() {
        String output = accum.toString();
        accum.delete(0, accum.length());
        return output;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
