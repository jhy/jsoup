package org.jsoup.internal;

import org.jsoup.helper.Validate;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import static org.jsoup.internal.SimpleBufferedInput.BufferPool;

/**
 A simple decoding InputStreamReader that recycles internal buffers.
 */
public class SimpleStreamReader extends Reader {
    private final InputStream in;
    private final CharsetDecoder decoder;
    private @Nullable ByteBuffer byteBuf; // null after close

    public SimpleStreamReader(InputStream in, Charset charset) {
        this.in = in;
        this.decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        byte[] buf = BufferPool.borrow(); // shared w/ SimpleBufferedInput, ControllableInput
        byteBuf = ByteBuffer.wrap(buf);
        byteBuf.flip(); // limit(0)
    }

    @Override
    public int read(char[] charArray, int off, int len) throws IOException {
        Validate.notNull(byteBuf); // can't read after close
        CharBuffer charBuf = CharBuffer.wrap(charArray, off, len);
        if (charBuf.position() != 0) charBuf = charBuf.slice();

        boolean readFully = false;
        while (true) {
            CoderResult result = decoder.decode(byteBuf, charBuf, readFully);
            if (result.isUnderflow()) {
                if (readFully || !charBuf.hasRemaining() || (charBuf.position() > 0) && !(in.available() > 0))
                    break;
                int read = bufferUp();
                if (read < 0) {
                    readFully = true;
                    if ((charBuf.position() == 0) && (!byteBuf.hasRemaining()))
                        break;
                }
                continue;
            }
            if (result.isOverflow()) break;
            result.throwException();
        }

        if (readFully) decoder.reset();
        if (charBuf.position() == 0) return -1;
        return charBuf.position();
    }

    private int bufferUp() throws IOException {
        assert byteBuf != null; // already validated ^
        byteBuf.compact();
        try {
            int pos = byteBuf.position();
            int remaining = (byteBuf.limit() - pos);
            int read = in.read(byteBuf.array(), byteBuf.arrayOffset() + pos, remaining);
            if (read < 0) return read;
            if (read == 0) throw new IOException("Underlying input stream returned zero bytes");
            byteBuf.position(pos + read);
        } finally {
            byteBuf.flip();
        }
        return byteBuf.remaining();
    }

    @Override
    public void close() throws IOException {
        if (byteBuf == null) return;
        BufferPool.release(byteBuf.array());
        byteBuf = null;
        in.close();
    }
}
