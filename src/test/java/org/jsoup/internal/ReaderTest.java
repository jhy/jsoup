package org.jsoup.internal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.jsoup.integration.ParseTest.getPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReaderTest {
    @Test void readerOfStringAndFile() throws IOException {
        // make sure that reading from a String and from a File produce the same bytes
        Path path = getPath("/fuzztests/garble.html");
        byte[] bytes = Files.readAllBytes(path);
        String fromBytes = new String(bytes, StandardCharsets.UTF_8);

        SimpleStreamReader streamReader = getReader(path);
        String fromStream = getString(streamReader);
        assertEquals(fromBytes, fromStream);

        SimpleStreamReader reader2 = getReader(path);
        CharacterReader cr = new CharacterReader(reader2);
        String fullRead = cr.consumeTo('X'); // does not exist in input
        assertEquals(fromBytes, fullRead);
    }

    @Test
    void testReadArithmeticBoundaries() throws IOException {
        byte[] data = "0123456789".getBytes(StandardCharsets.UTF_8);
        SimpleBufferedInput sbi = new SimpleBufferedInput(new ByteArrayInputStream(data));

        byte[] dest = new byte[10];

        // 1. Kill mutations in 'bufAvail = bufLength - bufPos'
        // Read exactly 1 byte to move bufPos
        int read1 = sbi.read(dest, 0, 1);
        assertEquals(1, read1);
        assertEquals((byte)'0', dest[0]);

        // 2. Kill mutations in 'read = Math.min(bufAvail, desiredLen)'
        // Request more than what is left in the buffer (but within stream limits)
        // If the Math.min logic is mutated, this will likely over-read or return wrong lengths
        byte[] largeDest = new byte[20];
        int readRemainder = sbi.read(largeDest, 0, 20);
        assertEquals(9, readRemainder); // Remaining: 123456789
        assertEquals((byte)'1', largeDest[0]);
        assertEquals((byte)'9', largeDest[8]);
    }

    @Test
    void testExactBufferExhaustion() throws IOException {
        // SimpleBufferedInput.BufferSize is 8192 by default.
        // We want to test the arithmetic when bufAvail is exactly 0.
        byte[] data = new byte[SimpleBufferedInput.BufferSize + 10];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 128);

        SimpleBufferedInput sbi = new SimpleBufferedInput(new ByteArrayInputStream(data));

        // Read exactly the full buffer size
        byte[] firstBatch = new byte[SimpleBufferedInput.BufferSize];
        int read = sbi.read(firstBatch, 0, SimpleBufferedInput.BufferSize);
        assertEquals(SimpleBufferedInput.BufferSize, read);

        // This second read triggers fill() because bufAvail == 0 (bufLength - bufPos == 0)
        // Mutations changing '-' to '+' or '*' in bufAvail calculation will fail here.
        byte[] secondBatch = new byte[10];
        int read2 = sbi.read(secondBatch, 0, 10);
        assertEquals(10, read2);
        assertEquals(data[SimpleBufferedInput.BufferSize], secondBatch[0]);
    }

    @Test
    void testDestinationOffsetArithmetic() throws IOException {
        byte[] data = "ABCDE".getBytes(StandardCharsets.UTF_8);
        SimpleBufferedInput sbi = new SimpleBufferedInput(new ByteArrayInputStream(data));

        byte[] dest = new byte[10];
        // Kill mutations in 'dest.length - offset' (the IndexOutOfBounds check)
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sbi.read(dest, 8, 3); // 8 + 3 > 10
        });

        // Kill mutations in System.arraycopy params (offset and read)
        // We write to the middle of the array
        int read = sbi.read(dest, 2, 3);
        assertEquals(3, read);
        assertEquals(0, dest[0]);    // Empty
        assertEquals('A', (char)dest[2]);
        assertEquals('B', (char)dest[3]);
        assertEquals('C', (char)dest[4]);
        assertEquals(0, dest[5]);    // Empty
    }

    @Test
    void testReadFullyAndEOF() throws IOException {
        byte[] data = "A".getBytes(StandardCharsets.UTF_8);
        SimpleBufferedInput sbi = new SimpleBufferedInput(new ByteArrayInputStream(data));

        sbi.read(new byte[1], 0, 1);

        // This read should hit the 'read <= 0' check and return -1
        // If 'read <= 0' is mutated to 'read < 0', this might hang or error
        int eof = sbi.read(new byte[1], 0, 1);
        assertEquals(-1, eof);
    }

    private static String getString(SimpleStreamReader streamReader) throws IOException {
        // read streamreader to a string:
        StringBuilder builder = new StringBuilder();
        char[] cbuffer = new char[1024];
        int read;
        while ((read = streamReader.read(cbuffer)) != -1) {
            builder.append(cbuffer, 0, read);
        }
        return builder.toString();
    }

    private static SimpleStreamReader getReader(Path path) throws IOException {
        // set up a chain as in when we parse: simplebufferedinput -> controllableinputstream -> simplestreamreader -> characterreader
        SimpleBufferedInput input = new SimpleBufferedInput(Files.newInputStream(path));
        ControllableInputStream stream = ControllableInputStream.wrap(input, 0);
        return new SimpleStreamReader(stream, StandardCharsets.UTF_8);
    }
}
