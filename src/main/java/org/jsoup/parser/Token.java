package org.jsoup.parser;

/**
 A Token of HTML. Internal use only.

 @author Jonathan Hedley, jonathan@hedley.net */
class Token {
    private String data;
    private Position pos;


    Token(String data, Position pos) {
        this.data = data;
        this.pos = pos;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token = (Token) o;

        if (data != null ? !data.equals(token.data) : token.data != null) return false;
        if (pos != null ? !pos.equals(token.pos) : token.pos != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (pos != null ? pos.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", data, pos);
    }
}
