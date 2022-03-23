package org.jsoup.helper;

public class BomCharset {
    public final String charset;
    public final boolean offset;

    public BomCharset(String charset, boolean offset) {
        this.charset = charset;
        this.offset = offset;
    }
}
