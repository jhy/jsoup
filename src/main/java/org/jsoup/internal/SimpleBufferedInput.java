package org.jsoup.internal;

import org.jsoup.helper.Validate;
import org.jspecify.annotations.Nullable;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.jsoup.internal.SharedConstants.DefaultBufferSize;

/**
 A simple implementation of a buffered input stream, in which we can control the byte[] buffer to recycle it. Not safe for
 use between threads; no sync or locks. The buffer is borrowed on initial demand in fill.
 @since 1.18.2
 */
class SimpleBufferedInput extends FilterInputStream {
    static final int BufferSize = DefaultBufferSize;
    static final SoftPool<byte[]> BufferPool = new SoftPool<>(() -> new byte[BufferSize]);
    private int capRemaining = Integer.MAX_VALUE; // how many bytes we are allowed to pull from the underlying stream

    private byte @Nullable [] byteBuf; // the byte buffer; recycled via SoftPool. Created in fill if required
    private int bufPos;
    private int bufLength;
    private int bufMark = -1; // mark set by ControllableInputStream; -1 when unset
    private boolean inReadFully = false; // true when the underlying inputstream has been read fully

    SimpleBufferedInput(@Nullable InputStream in) {
        super(in);
        if (in == null) inReadFully = true; // effectively an empty stream
    }

    @Override
    public int read() throws IOException {
        if (bufPos >= bufLength) {
            fill();
            if (bufPos >= bufLength)
                return -1;
        }
        return getBuf()[bufPos++] & 0xff;
    }

    @Override
    public int read(byte[] dest, int offset, int desiredLen) throws IOException {
        Validate.notNull(dest);
        if (offset < 0 || desiredLen < 0 || desiredLen > dest.length - offset) {
            throw new IndexOutOfBoundsException();
        } else if (desiredLen == 0) {
            return 0;
        }

        int bufAvail = bufLength - bufPos;
        if (bufAvail <= 0) { // can't serve from the buffer
            fill();
            bufAvail = bufLength - bufPos;
        }

        int read = Math.min(bufAvail, desiredLen);
        if (read <= 0) {
            return -1;
        }

        System.arraycopy(getBuf(), bufPos, dest, offset, read);
        bufPos += read;
        return read;
    }

    private void fill() throws IOException {
        if (inReadFully) return;
        if (byteBuf == null) { // get one on first demand
            byteBuf = BufferPool.borrow();
        }

        compact();
        bufLength = bufPos;
        int toRead = Math.min(byteBuf.length - bufPos, capRemaining);
        if (toRead <= 0) return;
        int read = in.read(byteBuf, bufPos, toRead);
        if (read > 0) {
            bufLength = read + bufPos;
            capRemaining -= read;
            while (byteBuf.length - bufLength > 0 && capRemaining > 0) { // read in more if we have space, without blocking
                if (in.available() < 1) break;
                toRead = Math.min(byteBuf.length - bufLength, capRemaining);
                if (toRead <= 0) break;
                read = in.read(byteBuf, bufLength, toRead);
                if (read <= 0) break;
                bufLength += read;
                capRemaining -= read;
            }
        }
        if (read == -1) inReadFully = true;
    }

    byte[] getBuf() {
        Validate.notNull(byteBuf);
        return byteBuf;
    }

    /**
     Check if the underlying InputStream has been read fully. There may still content in this buffer to be consumed.
     @return true if the underlying inputstream has been read fully.
     */
    boolean baseReadFully() {
        return inReadFully;
    }

    void resetFullyRead() {
        if (in != null) // for null-wrapped streams, leave as fully read to avoid fill() on a null input
            inReadFully = false;
    }

    @Override
    public int available() throws IOException {
        int buffered = (byteBuf != null) ? (bufLength - bufPos) : 0;
        if (buffered > 0) {
            return buffered; // doesn't include those in.available(), but mostly used as a block test
        }
        int avail = inReadFully ? 0 : in.available();
        return avail;
    }

    void capRemaining(int newRemaining) {
        capRemaining = Math.max(0, newRemaining);
    }

    void setMark() {
        bufMark = bufPos;
    }

    void rewindToMark() throws IOException {
        if (bufMark < 0)
            throw new IOException("Resetting to invalid mark");
        bufPos = bufMark;
    }

    void clearMark() {
        bufMark = -1;
    }

    private void compact() {
        if (byteBuf == null || bufPos == 0) return;
        int keepFrom = bufMark >= 0 ? bufMark : bufPos;
        if (keepFrom <= 0) return;

        int remaining = bufLength - keepFrom;
        if (remaining > 0) {
            System.arraycopy(byteBuf, keepFrom, byteBuf, 0, remaining);
        }
        bufLength = remaining;
        bufPos -= keepFrom;
        if (bufMark >= 0) {
            bufMark -= keepFrom;
        }
    }

    @Override
    public void close() throws IOException {
        if (in != null) super.close();
        if (byteBuf == null) return; // already closed, or never allocated
        BufferPool.release(byteBuf); // return the buffer to the pool
        byteBuf = null; // NPE further attempts to read
    }
}
