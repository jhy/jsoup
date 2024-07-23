package org.jsoup.integration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 Tests fixes for issues raised by the <a href="https://oss-fuzz.com/testcases?project=jsoup">OSS Fuzz project</a>. As
 some of these are timeout tests - run each file 100 times and ensure under time.
 */
public class FuzzFixesIT {
    static int numIters = 50;
    static int timeout = 30; // external fuzzer is set to 60 for 100 runs
    static Path testDir = ParseTest.getPath("/fuzztests/");

    private static Stream<Path> testPaths() throws IOException {
        return Files.list(testDir);
    }

    @Disabled // disabled, as these soak up build time and the outcome oughtn't change unless we are refactoring the tree builders. manually execute as desired.
    @ParameterizedTest
    @MethodSource("testPaths")
    void testHtmlParse(Path path) throws IOException {
        long startTime = System.currentTimeMillis();
        long completeBy = startTime + timeout * 1000L;

        for (int i = 0; i < numIters; i++) {
            Document doc = Jsoup.parse(path, "UTF-8", "https://example.com/");
            assertNotNull(doc);
            if (System.currentTimeMillis() > completeBy)
                Assertions.fail(String.format("Timeout: only completed %d iters of [%s] in %d seconds", i, path.getFileName().toString(), timeout));
        }
    }
    
    @Disabled // disabled, as these soak up build time and the outcome oughtn't change unless we are refactoring the tree builders. manually execute as desired.
    @ParameterizedTest
    @MethodSource("testPaths")
    void testXmlParse(Path path) throws IOException {
        long startTime = System.currentTimeMillis();
        long completeBy = startTime + timeout * 1000L;

        for (int i = 0; i < numIters; i++) {
            Document doc = Jsoup.parse(path, "UTF-8", "https://example.com/", Parser.xmlParser());
            assertNotNull(doc);
            if (System.currentTimeMillis() > completeBy)
                Assertions.fail(String.format("Timeout: only completed %d iters of [%s] in %d seconds", i, path.getFileName().toString(), timeout));
        }
    }
}
