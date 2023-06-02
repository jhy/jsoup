package org.jsoup;

import java.io.IOException;

/**
 * @deprecated Use {@link java.io.UncheckedIOException} instead.
 */
@Deprecated
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
