package org.jsoup;

import java.io.IOException;

public class UncheckedIOException extends RuntimeException {
    private static final long serialVersionUID = 5295483349093983264L;

    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    public UncheckedIOException(String message) {
        super(new IOException(message));
    }

    public IOException ioException() {
        return (IOException) getCause();
    }
}
