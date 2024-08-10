package org.jsoup.internal;

import org.jsoup.helper.Validate;
import org.jspecify.annotations.Nullable;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.jsoup.internal.SharedConstants.DefaultBufferSize;

/**
 A simple implemented of a buffered input stream, in which we can control the byte[] buffer to recycle it. Not safe for
 use between threads; no sync or locks. The buffer is borrowed on initial demand in fill.
 @since 1.18.2
 */
class SimpleBufferedInput extends FilterInputStream {
    static final int BufferSize = DefaultBufferSize;
    static final SoftPool<byte[]> BufferPool = new SoftPool<>(() -> new byte[BufferSize]);

    private byte @Nullable [] byteBuf; // the byte buffer; recycled via SoftPool. Created in fill if required
    private int bufPos;
    private int bufLength;
    private int bufMark = -1;
    private boolean inReadFully = false; // true when the underlying inputstream has been read fully

    SimpleBufferedInput(InputStream in) {
        super(in);
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
            if (!inReadFully && bufMark < 0) {
                // skip creating / copying into a local buffer; just pass through
                int read = in.read(dest, offset, desiredLen);
                closeIfDone(read);
                return read;
            }
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

        if (bufMark < 0) { // no mark, can lose buffer (assumes we've read to bufLen)
            bufPos = 0;
        } else if (bufPos >= BufferSize) { // no room left in buffer
            if (bufMark > 0) { // can throw away early part of the buffer
                int size = bufPos - bufMark;
                System.arraycopy(byteBuf, bufMark, byteBuf, 0, size);
                bufPos = size;
                bufMark = 0;
            } else { // invalidate mark
                bufMark = -1;
                bufPos = 0;
            }
        }
        bufLength = bufPos;
        int read = in.read(byteBuf, bufPos, byteBuf.length - bufPos);
        if (read > 0) {
            bufLength = read + bufPos;
            while (byteBuf.length - bufLength > 0) { // read in more if we have space, without blocking
                if (in.available() < 1) break;
                read = in.read(byteBuf, bufLength, byteBuf.length - bufLength);
                if (read <= 0) break;
                bufLength += read;
            }
        }
        closeIfDone(read);
    }

    private void closeIfDone(int read) throws IOException {
        if (read == -1) {
            inReadFully = true;
            super.close(); // close underlying stream immediately; frees resources a little earlier
        }
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

    @Override
    public int available() throws IOException {
        if (byteBuf != null && bufLength - bufPos > 0)
            return bufLength - bufPos; // doesn't include those in.available(), but mostly used as a block test
        return inReadFully ? 0 : in.available();
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // explicitly not synced
    @Override
    public void mark(int readlimit) {
        if (readlimit > BufferSize) {
            throw new IllegalArgumentException("Read-ahead limit is greater than buffer size");
        }
        bufMark = bufPos;
    }

    @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod") // explicitly not synced
    @Override
    public void reset() throws IOException {
        if (bufMark < 0)
            throw new IOException("Resetting to invalid mark");
        bufPos = bufMark;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (byteBuf == null) return; // already closed, or never allocated
        BufferPool.release(byteBuf); // return the buffer to the pool
        byteBuf = null; // NPE further attempts to read
    }
}
