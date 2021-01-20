package org.jsoup;

import java.io.IOException;

/**
 * Signals that a HTTP request resulted in a not OK HTTP response.
 */
public class HttpStatusException extends IOException {
    private final int statusCode;
    private final String url;

    public HttpStatusException(String message, int statusCode, String url) {
        super(message + ". Status=" + statusCode + ", URL=[" + url + "]");
        this.statusCode = statusCode;
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getUrl() {
        return url;
    }
}
