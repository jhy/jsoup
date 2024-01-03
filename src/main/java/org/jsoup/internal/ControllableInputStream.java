package org.jsoup.internal;

import org.jsoup.helper.DataUtil;
import org.jsoup.helper.Validate;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import static org.jsoup.internal.SharedConstants.DefaultBufferSize;

/**
 * A jsoup internal class (so don't use it as there is no contract API) that enables controls on a Buffered Input Stream,
 * namely a maximum read size, and the ability to Thread.interrupt() the read.
 */
// reimplemented from ConstrainableInputStream for JDK21 - extending BufferedInputStream will pin threads during read
public class ControllableInputStream extends FilterInputStream {
    private final BufferedInputStream buff;
    private final boolean capped;
    private final int maxSize;
    private long startTime;
    private long timeout = 0; // optional max time of request
    private int remaining;
    private int markPos;
    private boolean interrupted;

    private ControllableInputStream(BufferedInputStream in, int maxSize) {
        super(in);
        Validate.isTrue(maxSize >= 0);
        buff = in;
        capped = maxSize != 0;
        this.maxSize = maxSize;
        remaining = maxSize;
        markPos = -1;
        startTime = System.nanoTime();
    }

    /**
     * If this InputStream is not already a ControllableInputStream, let it be one.
     * @param in the input stream to (maybe) wrap
     * @param bufferSize the buffer size to use when reading
     * @param maxSize the maximum size to allow to be read. 0 == infinite.
     * @return a controllable input stream
     */
    public static ControllableInputStream wrap(InputStream in, int bufferSize, int maxSize) {
        if (in instanceof ControllableInputStream)
            return (ControllableInputStream) in;
        else if (in instanceof BufferedInputStream)
            return new ControllableInputStream((BufferedInputStream) in, maxSize);
        else
            return new ControllableInputStream(new BufferedInputStream(in, bufferSize), maxSize);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (interrupted || capped && remaining <= 0)
            return -1;
        if (Thread.currentThread().isInterrupted()) {
            // interrupted latches, because parse() may call twice
            interrupted = true;
            return -1;
        }
        if (expired())
            throw new SocketTimeoutException("Read timeout");

        if (capped && len > remaining)
            len = remaining; // don't read more than desired, even if available

        try {
            final int read = super.read(b, off, len);
            remaining -= read;
            return read;
        } catch (SocketTimeoutException e) {
            if (expired())
                throw e;
            return 0;
        }
    }

    /**
     * Reads this inputstream to a ByteBuffer. The supplied max may be less than the inputstream's max, to support
     * reading just the first bytes.
     */
    public static ByteBuffer readToByteBuffer(InputStream in, int max) throws IOException {
        Validate.isTrue(max >= 0, "maxSize must be 0 (unlimited) or larger");
        Validate.notNull(in);
        final boolean localCapped = max > 0; // still possibly capped in total stream
        final int bufferSize = localCapped && max < DefaultBufferSize ? max : DefaultBufferSize;
        final byte[] readBuffer = new byte[bufferSize];
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream(bufferSize);

        int read;
        int remaining = max;
        while (true) {
            read = in.read(readBuffer, 0, localCapped ? Math.min(remaining, bufferSize) : bufferSize);
            if (read == -1) break;
            if (localCapped) { // this local byteBuffer cap may be smaller than the overall maxSize (like when reading first bytes)
                if (read >= remaining) {
                    outStream.write(readBuffer, 0, remaining);
                    break;
                }
                remaining -= read;
            }
            outStream.write(readBuffer, 0, read);
        }
        return ByteBuffer.wrap(outStream.toByteArray());
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // not synchronized in later JDKs
    @Override public void reset() throws IOException {
        super.reset();
        remaining = maxSize - markPos;
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // not synchronized in later JDKs
    @Override public void mark(int readlimit) {
        super.mark(readlimit);
        markPos = maxSize - remaining;
    }

    public ControllableInputStream timeout(long startTimeNanos, long timeoutMillis) {
        this.startTime = startTimeNanos;
        this.timeout = timeoutMillis * 1000000;
        return this;
    }

    private boolean expired() {
        if (timeout == 0)
            return false;

        final long now = System.nanoTime();
        final long dur = now - startTime;
        return (dur > timeout);
    }

    public BufferedInputStream inputStream() {
        return buff;
    }
}
