package org.jsoup;

import java.io.IOException;

/**
 * @deprecated Use {@link java.io.UncheckedIOException} instead. This class acted as a compatibility shim for Java
 * versions prior to 1.8.
 */
@Deprecated
public class UncheckedIOException extends java.io.UncheckedIOException {
    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    public UncheckedIOException(String message) {
        super(new IOException(message));
    }

    public IOException ioException() {
        return getCause();
    }
}
