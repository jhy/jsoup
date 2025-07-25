package org.jsoup.internal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.jsoup.integration.ParseTest.getPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
