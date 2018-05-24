package org.jsoup.internal;

import org.jsoup.Jsoup;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class ConstrainableInputStreamTest {
    // todo - move these all to local jetty, don't ignore

    @Test
    public void remainingAfterFirstRead() throws IOException {
        int bufferSize = 5 * 1024;
        int capSize = 100 * 1024;

        String url = "http://direct.infohound.net/tools/large.html"; // 280 K
        BufferedInputStream inputStream = Jsoup.connect(url).maxBodySize(capSize)
            .execute().bodyStream();

        assertTrue(inputStream instanceof ConstrainableInputStream);
        ConstrainableInputStream stream = (ConstrainableInputStream) inputStream;

        // simulates parse which does a limited read first
        stream.mark(bufferSize);
        ByteBuffer firstBytes = stream.readToByteBuffer(bufferSize);

        byte[] array = firstBytes.array();
        String firstText = new String(array, "UTF-8");
        assertTrue(firstText.startsWith("<html><head><title>Large"));
        assertEquals(bufferSize, array.length);

        boolean fullyRead = stream.read() == -1;
        assertFalse(fullyRead);

        // reset and read again
        stream.reset();
        ByteBuffer fullRead = stream.readToByteBuffer(0);
        byte[] fullArray = fullRead.array();
        assertEquals(capSize, fullArray.length);
        String fullText = new String(fullArray, "UTF-8");
        assertTrue(fullText.startsWith(firstText));
    }

    @Test
    public void noLimitAfterFirstRead() throws IOException {
        int bufferSize = 5 * 1024;

        String url = "http://direct.infohound.net/tools/large.html"; // 280 K
        BufferedInputStream inputStream = Jsoup.connect(url).execute().bodyStream();

        assertTrue(inputStream instanceof ConstrainableInputStream);
        ConstrainableInputStream stream = (ConstrainableInputStream) inputStream;

        // simulates parse which does a limited read first
        stream.mark(bufferSize);
        ByteBuffer firstBytes = stream.readToByteBuffer(bufferSize);
        byte[] array = firstBytes.array();
        String firstText = new String(array, "UTF-8");
        assertTrue(firstText.startsWith("<html><head><title>Large"));
        assertEquals(bufferSize, array.length);

        // reset and read fully
        stream.reset();
        ByteBuffer fullRead = stream.readToByteBuffer(0);
        byte[] fullArray = fullRead.array();
        assertEquals(280735, fullArray.length);
        String fullText = new String(fullArray, "UTF-8");
        assertTrue(fullText.startsWith(firstText));

    }
}
