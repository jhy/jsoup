package org.jsoup.internal;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ControllableInputStreamTest {

    @Test
    void respectsMaxCapDuringFill() throws IOException {
        byte[] data = "0123456789".getBytes(); // 10 bytes
        CountingInputStream counting = new CountingInputStream(new ByteArrayInputStream(data));

        ControllableInputStream in = ControllableInputStream.wrap(counting, 5); // cap at 5 bytes
        byte[] buf = new byte[10];

        int read = in.read(buf);
        assertEquals(5, read, "should only read up to cap");
        assertEquals(5, counting.count, "underlying stream should not be pulled past cap");
        assertFalse(in.baseReadFully(), "cap hit is not EOF");

        int second = in.read(buf);
        assertEquals(-1, second, "further reads return -1 once cap is exhausted");
        assertFalse(in.baseReadFully(), "still not true EOF");
        in.close();
    }

    @Test
    void compactsBufferWithActiveMark() throws IOException {
        int size = SharedConstants.DefaultBufferSize * 2;
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) data[i] = (byte) (i % 256);

        ControllableInputStream in = ControllableInputStream.wrap(new ByteArrayInputStream(data), 0);

        byte[] first = new byte[500];
        assertEquals(500, in.read(first));

        in.mark(SharedConstants.DefaultBufferSize); // mark at logical pos 500

        byte[] consume = new byte[SharedConstants.DefaultBufferSize];
        int firstRead = in.read(consume); // serves remainder of current buffer (BufferSize - 500)
        assertEquals(SharedConstants.DefaultBufferSize - 500, firstRead);

        byte[] more = new byte[1000];
        int secondRead = in.read(more); // triggers fill() with active mark, then consumes from freshly filled buffer
        assertEquals(SharedConstants.DefaultBufferSize - firstRead, secondRead);

        in.reset(); // should rewind to mark despite prior compaction

        byte[] reread = new byte[1000];
        assertEquals(1000, in.read(reread));
        for (int i = 0; i < reread.length; i++) {
            assertEquals(data[500 + i], reread[i], "byte mismatch at " + i);
        }
        in.close();
    }

    @Test
    void distinguishesExactLimitFromTruncation() throws IOException {
        CountingInputStream shorterSource = new CountingInputStream(new ByteArrayInputStream(new byte[4]));
        ControllableInputStream shorter = ControllableInputStream.wrap(shorterSource, 5);
        assertEquals(4, readCount(shorter.inputStream()));
        assertEquals(4, shorterSource.count);
        assertFalse(shorter.isTruncated());

        CountingInputStream exactSource = new CountingInputStream(new ByteArrayInputStream(new byte[5]));
        ControllableInputStream exact = ControllableInputStream.wrap(exactSource, 5);
        assertEquals(5, readCount(exact.inputStream()));
        assertEquals(5, exactSource.count);
        assertFalse(exact.isTruncated());

        CountingInputStream longerSource = new CountingInputStream(new ByteArrayInputStream(new byte[6]));
        ControllableInputStream longer = ControllableInputStream.wrap(longerSource, 5);
        assertEquals(5, readCount(longer.inputStream()));
        assertEquals(6, longerSource.count);
        assertTrue(longer.isTruncated());
    }

    @Test
    void retainsLookaheadWhenLimitIsRaised() throws IOException {
        byte[] data = "abcdef".getBytes();
        ControllableInputStream in = ControllableInputStream.wrap(new ByteArrayInputStream(data), 5);
        in.mark(data.length);
        assertEquals(5, readCount(in));
        assertTrue(in.isTruncated());

        in.reset();
        in.max(0);
        byte[] reread = new byte[data.length];
        assertEquals(5, in.read(reread));
        assertEquals(1, in.read(reread, 5, 1));
        assertArrayEquals(data, reread);
        assertFalse(in.isTruncated());
        in.close();
    }

    @Test
    void readAndSkipRespectLimit() throws IOException {
        ControllableInputStream single = ControllableInputStream.wrap(
            new ByteArrayInputStream(new byte[] {42, 43}), 1);
        assertEquals(42, single.read());
        assertEquals(-1, single.read());
        assertTrue(single.isTruncated());
        single.close();

        ControllableInputStream skipped = ControllableInputStream.wrap(new ByteArrayInputStream(new byte[200]), 100);
        try (InputStream stream = skipped.inputStream()) {
            assertEquals(100, stream.skip(1000));
            assertEquals(-1, stream.read());
        }
        assertTrue(skipped.isTruncated());
    }

    @Test
    void largeSkipSaturatesProgress() throws IOException {
        long size = (long) Integer.MAX_VALUE + 1;
        ControllableInputStream in = ControllableInputStream.wrap(new VirtualInputStream(size), 0);
        AtomicBoolean negative = new AtomicBoolean();
        AtomicInteger lastProcessed = new AtomicInteger();
        AtomicInteger lastTotal = new AtomicInteger();
        AtomicInteger lastPercent = new AtomicInteger();
        in.onProgress(-1, (processed, total, percent, context) -> {
            if (processed < 0) negative.set(true);
            lastProcessed.set(processed);
            lastTotal.set(total);
            lastPercent.set((int) percent);
        }, in);

        try (InputStream stream = in.inputStream()) {
            assertEquals(size, stream.skip(size));
            assertEquals(-1, stream.read());
        }
        assertFalse(negative.get());
        assertEquals(Integer.MAX_VALUE, lastProcessed.get());
        assertEquals(Integer.MAX_VALUE, lastTotal.get());
        assertEquals(100, lastPercent.get());
    }

    private static long readCount(InputStream in) throws IOException {
        long count = 0;
        byte[] buffer = new byte[1024 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) count += read;
        return count;
    }

    private static final class VirtualInputStream extends InputStream {
        private long remaining;

        VirtualInputStream(long size) {
            remaining = size;
        }

        @Override
        public int read() {
            if (remaining == 0) return -1;
            remaining--;
            return 0;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (remaining == 0) return -1;
            int read = (int) Math.min(remaining, length);
            remaining -= read;
            return read;
        }
    }

    private static final class CountingInputStream extends FilterInputStream {
        int count = 0;

        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int r = super.read(b, off, len);
            if (r > 0) count += r;
            return r;
        }

        @Override
        public int read() throws IOException {
            int r = super.read();
            if (r != -1) count++;
            return r;
        }
    }
}
