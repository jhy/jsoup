package org.jsoup.parser;

/**
 Runtime exception to wrap up unlikely checked exceptions thrown when parsing.

 @author Jonathan Hedley, jonathan@hedley.net */
public class ParserRuntimeException extends RuntimeException {
    private Position pos;

    public ParserRuntimeException(String message, Throwable cause, Position pos) {
        super(message, cause);
        this.pos = pos;
    }

    public Position getPos() {
        return pos;
    }
}
