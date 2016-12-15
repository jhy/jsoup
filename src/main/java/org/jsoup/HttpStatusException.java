package org.jsoup;

import java.io.IOException;

/**
 * Signals that a HTTP request resulted in a not OK HTTP response.
 */
public class HttpStatusException extends IOException {
    private int statusCode;
    private String url;
    private String errorDetail;
    
    public HttpStatusException(String message, int statusCode, String url,String errorDetail) {
        super(message);
        this.statusCode = statusCode;
        this.url = url;
        this.errorDetail = errorDetail;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return super.toString() + ". Status=" + statusCode + ", URL=" + url+", errorDetail="+errorDetail;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

  
}
