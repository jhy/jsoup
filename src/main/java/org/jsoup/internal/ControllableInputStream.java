package org.jsoup.internal;

import org.jsoup.Progress;
import org.jsoup.helper.Validate;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import static org.jsoup.internal.SharedConstants.DefaultBufferSize;

/**
 * A jsoup internal class (so don't use it as there is no contract API) that enables controls on a buffered input stream,
 * namely a maximum read size, and the ability to Thread.interrupt() the read.
 */
// reimplemented from ConstrainableInputStream for JDK21 - extending BufferedInputStream will pin threads during read
public class ControllableInputStream extends FilterInputStream {
    private final SimpleBufferedInput buff; // super.in, but typed as SimpleBufferedInput
    private int maxSize;                    // logical cap exposed to callers (0 == unlimited)
    private long startTime;                 // start time for timeout checks, nanos
    private long timeout = 0;               // optional max time of request
    private int remaining;                  // how many bytes may still be returned to caller under the current cap
    private int markPos;                    // logical readPos snapshot for InputStream.mark/reset (not a buffer cursor)
    private boolean interrupted;            // true if Thread.interrupted() was detected, used to latch interrupted state
    private boolean truncated;              // true if there is content beyond the body cap
    private boolean allowClose = true;      // for cases where we want to re-read the input, can ignore .close() from the parser
    private final byte[] singleByte = new byte[1]; // avoids allocation when routing single-byte reads through controls

    // if we are tracking progress, will have the expected content length and progress callback state
    private @Nullable ProgressState<?> progress;
    private int contentLength = -1;         // expected content length for progress; -1 == unknown
    private int readPos = 0;                // amount read; can be reset()

    private ControllableInputStream(SimpleBufferedInput in, int maxSize) {
        super(in);
        Validate.isTrue(maxSize >= 0);
        buff = in;
        this.maxSize = maxSize;
        remaining = maxSize;
        markPos = -1;
        startTime = System.nanoTime();
    }

    /**
     * If this InputStream is not already a ControllableInputStream, let it be one.
     * @param in the input stream to (maybe) wrap. A {@code null} input will create an empty wrapped stream.
     * @param maxSize the maximum size to allow to be read. 0 == infinite.
     * @return a controllable input stream
     */
    public static ControllableInputStream wrap(@Nullable InputStream in, int maxSize) {
        // bufferSize currently unused; consider implementing as a min size in the SoftPool recycler
        if (in instanceof ControllableInputStream)
            return (ControllableInputStream) in;
        else
            return new ControllableInputStream(new SimpleBufferedInput(in), maxSize);
    }

    /**
     * If this InputStream is not already a ControllableInputStream, let it be one.
     * @param in the input stream to (maybe) wrap
     * @param bufferSize the buffer size to use when reading
     * @param maxSize the maximum size to allow to be read. 0 == infinite.
     * @return a controllable input stream
     */
    public static ControllableInputStream wrap(InputStream in, int bufferSize, int maxSize) {
        // todo - bufferSize currently unused; consider implementing as a min size in the SoftPool recycler; or just deprecate if always DefaultBufferSize
        return wrap(in, maxSize);
    }

    @Override
    public int read() throws IOException {
        int read = read(singleByte, 0, 1);
        return read == -1 ? -1 : singleByte[0] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (readPos == 0) emitProgress(); // emits a progress

        boolean capped = maxSize != 0;
        if (interrupted)
            return -1;
        if (Thread.currentThread().isInterrupted()) {
            // interrupted latches, because parse() may call twice
            interrupted = true;
            return -1;
        }
        if (capped && remaining <= 0) {
            if (checkTruncated()) return -1;
            contentLength = readPos;
            emitProgress();
            return -1;
        }

        if (capped && len > remaining)
            len = remaining; // don't read more than desired, even if available
        if (capped) buff.capRemaining(remaining);
        else buff.uncap();

        while (true) { // loop trying to read until we get some data or hit the overall timeout, if we have one
            if (expired())
                throw new SocketTimeoutException("Read timeout");

            try {
                final int read = super.read(b, off, len);
                if (read == -1) { // completed
                    contentLength = readPos;
                } else {
                    if (capped && read > 0) {
                        remaining -= read; // track bytes returned to the caller
                    }
                    // todo: use long progress values in the public API; saturate until that is available
                    readPos = read > Integer.MAX_VALUE - readPos ? Integer.MAX_VALUE : readPos + read;
                }
                emitProgress();
                return read;
            } catch (SocketTimeoutException e) {
                if (expired() || timeout == 0)
                    throw e;
            }
        }
    }

    @Override
    public long skip(long requested) throws IOException {
        // implemented here so our cap accounting aligns
        if (requested <= 0) return 0;

        byte[] skipBuffer = SimpleBufferedInput.BufferPool.borrow();
        long skipped = 0;
        try {
            while (skipped < requested) {
                int read = read(skipBuffer, 0, (int) Math.min(requested - skipped, skipBuffer.length));
                if (read == -1) break;
                skipped += read;
            }
            return skipped;
        } finally {
            SimpleBufferedInput.BufferPool.release(skipBuffer);
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Reads this inputstream to a ByteBuffer. The supplied max may be less than the inputstream's max, to support
     * reading just the first bytes.
     */
    public static ByteBuffer readToByteBuffer(InputStream in, int max) throws IOException {
        Validate.isTrue(max >= 0, "maxSize must be 0 (unlimited) or larger");
        Validate.notNull(in);
        final boolean capped = max > 0;
        final byte[] readBuf = SimpleBufferedInput.BufferPool.borrow(); // Share the same byte[] pool as SBI
        final int outSize = capped ? Math.min(max, DefaultBufferSize) : DefaultBufferSize;
        ByteBuffer outBuf = ByteBuffer.allocate(outSize);

        try {
            int remaining = max;
            int read;
            while ((read = in.read(readBuf, 0, capped ? Math.min(remaining, DefaultBufferSize) : DefaultBufferSize)) != -1) {
                if (outBuf.remaining() < read) { // needs to grow
                    int newCapacity = (int) Math.max(outBuf.capacity() * 1.5, outBuf.capacity() + read);
                    ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
                    outBuf.flip();
                    newBuffer.put(outBuf);
                    outBuf = newBuffer;
                }
                outBuf.put(readBuf, 0, read);
                if (capped) {
                    remaining -= read;
                    if (remaining <= 0) break;
                }
            }
            outBuf.flip(); // Prepare the buffer for reading
            return outBuf;
        } finally {
            SimpleBufferedInput.BufferPool.release(readBuf);
        }
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // not synchronized in later JDKs
    @Override public void reset() throws IOException {
        if (markPos < 0) throw new IOException("Resetting to invalid mark");
        buff.rewindToMark();
        buff.clearMark();
        truncated = false;
        if (maxSize != 0) {
            remaining = maxSize - markPos;
            buff.capRemaining(remaining);
        } else {
            remaining = 0;
            buff.uncap();
        }
        readPos = markPos; // readPos is used for progress emits
        markPos = -1;
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // not synchronized in later JDKs
    @Override public void mark(int readlimit) {
        markPos = readPos;
        buff.setMark();
    }

    /**
     Check if the underlying InputStream has been read fully. There may still content in buffers to be consumed, and
     read methods may return -1 if hit the read limit.
     @return true if the underlying inputstream has been read fully.
     */
    public boolean baseReadFully() {
        return buff.baseReadFully();
    }

    public void resetFullyRead() {
        buff.resetFullyRead();
    }

    /**
     Get the max size of this stream (how far at most will be read from the underlying stream)
     * @return the max size
     */
    public int max() {
        return maxSize;
    }

    public void max(int newMax) {
        remaining += newMax - maxSize; // update remaining to reflect the difference in the new maxsize
        if (remaining < 0) remaining = 0;
        if (newMax == 0 || newMax > maxSize) truncated = false;
        maxSize = newMax;
        if (newMax == 0) buff.uncap();
        else buff.capRemaining(remaining);
    }

    /**
     Check if content remains beyond the configured cap.
     */
    public boolean checkTruncated() throws IOException {
        while (!truncated && maxSize != 0 && remaining <= 0) {
            if (expired()) throw new SocketTimeoutException("Read timeout");
            try {
                truncated = buff.hasMore();
                break;
            } catch (SocketTimeoutException e) {
                if (expired() || timeout == 0) throw e;
            }
        }
        return truncated;
    }

    /**
     Returns whether content was found beyond the configured cap.
     */
    public boolean isTruncated() {
        return truncated;
    }

    public void allowClose(boolean allowClose) {
        this.allowClose = allowClose;
    }

    @Override public void close() throws IOException {
        if (allowClose) super.close();
    }

    public ControllableInputStream timeout(long startTimeNanos, long timeoutMillis) {
        this.startTime = startTimeNanos;
        this.timeout = timeoutMillis * 1000000;
        return this;
    }

    private void emitProgress() {
        ProgressState<?> progress = this.progress;
        if (progress == null) return;
        // calculate percent complete if contentLength > 0 (and cap to 100.0 if totalRead > contentLength):
        float percent = contentLength > 0 ? Math.min(100f, readPos * 100f / contentLength) : 0;
        progress.emit(readPos, contentLength, percent);
        if (percent == 100.0f) this.progress = null; // detach once we reach 100%, so that any subsequent buffer hits don't report 100 again
    }

    public <ProgressContext> ControllableInputStream onProgress(int contentLength, Progress<ProgressContext> callback, ProgressContext context) {
        Validate.notNull(callback);
        Validate.notNull(context);
        this.contentLength = contentLength;
        this.progress = new ProgressState<>(callback, context);
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
        // called via HttpConnection.Response.bodyStream(), needs an OG BufferedInputStream
        return new BufferedInputStream(this); // this, not buff, so that return is constrained
    }

    private static class ProgressState<ProgressContext> {
        private final Progress<ProgressContext> callback;
        private final ProgressContext context;

        ProgressState(Progress<ProgressContext> callback, ProgressContext context) {
            this.callback = callback;
            this.context = context;
        }

        void emit(int processed, int total, float percent) {
            callback.onProgress(processed, total, percent, context);
        }
    }
}
