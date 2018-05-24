package org.jsoup.hamcrest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.BeforeClass;

public abstract class HtmlBaseTest {
    protected static String source;

    @BeforeClass
    public static void setupClass() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        FluentExampleTest.class.getResourceAsStream("/examples/index.html"), StandardCharsets.UTF_8))) {
            source = reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
