package org.jsoup.internal;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

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
