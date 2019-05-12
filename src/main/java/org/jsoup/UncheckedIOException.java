package org.jsoup;

import java.io.IOException;

public class UncheckedIOException extends RuntimeException {
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
