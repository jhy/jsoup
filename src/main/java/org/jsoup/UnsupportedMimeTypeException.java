package org.jsoup;

import java.io.IOException;

/**
 * Signals that a HTTP response returned a mime type that is not supported.
 */
public class UnsupportedMimeTypeException extends IOException {
    private final String mimeType;
    private final String url;

    public UnsupportedMimeTypeException(String message, String mimeType, String url) {
        super(message);
        this.mimeType = mimeType;
        this.url = url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return super.toString() + ". Mimetype=" + mimeType + ", URL="+url;
    }
}
