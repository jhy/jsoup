package org.jsoup.internal;

import org.jsoup.helper.Validate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A jsoup internal class (so don't use it as there is no contract API) that enables constraints on an Input Stream,
 * namely a maximum read size, and the ability to Thread.interrupt() the read.
 */
public final class ConstrainableInputStream extends BufferedInputStream {
    private final boolean capped;
    private int remaining;

    public ConstrainableInputStream(InputStream in, int bufferSize, int maxSize) {
        super(in, bufferSize);
        Validate.isTrue(maxSize >= 0);
        remaining = maxSize;
        capped = maxSize != 0;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (Thread.interrupted() || remaining < 0)
            return -1;

        final int read = super.read(b, off, len);
        if (capped) {
            remaining -= read;
        }
        return read;
    }
}
