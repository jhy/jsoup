package org.jsoup.internal;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static org.jsoup.internal.StringUtil.normaliseWhitespace;
import static org.jsoup.internal.StringUtil.resolve;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class StringUtilTest {

    @ParameterizedTest
    @MethodSource("joinTestArgumentsProvider")
    public void join(String expected, Collection list) {
        assertEquals(expected, StringUtil.join(list, " "));
    }

    static Stream<Arguments> joinTestArgumentsProvider() {
        return Stream.of(
                arguments("", Collections.singletonList("")),
                arguments("one", Collections.singletonList("one")),
                arguments("one two three", Arrays.asList("one", "two", "three"))
        );
    }

    @ParameterizedTest
    @CsvSource({
            "''                              , 0",
            "' '                             , 1",
            "'  '                            , 2",
            "'               '               , 15",
            "'                              ', 45" // we tap out at 30
    })
    public void padding(String expected, int width) {
        assertEquals(expected, StringUtil.padding(width));
    }

    @Test public void paddingInACan() {
        String[] padding = StringUtil.padding;
        assertEquals(21, padding.length);
        for (int i = 0; i < padding.length; i++) {
            assertEquals(i, padding[i].length());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"      ", "   \r\n  "})
    public void isBlank_validValues(String value) {
        assertTrue(StringUtil.isBlank(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "hello", "   hello   "})
    public void isBlank_invalidValues(String value) {
        assertFalse(StringUtil.isBlank(value));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", "123 546", "hello", "123.334"})
    public void isNumeric_invalidValues(String value) {
        assertFalse(StringUtil.isNumeric(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "1234"})
    public void isNumeric_validValues(String value) {
        assertTrue(StringUtil.isNumeric(value));
    }

    @ParameterizedTest
    @ValueSource(ints = {'\t', '\n', '\r', '\f', ' '})
    public void isWhitespace_validValues(int value) {
        assertTrue(StringUtil.isWhitespace(value));
    }

    @ParameterizedTest
    @ValueSource(ints = {'\u00a0', '\u2000', '\u3000'})
    public void isWhitespace_invalidValues(int value) {
        assertFalse(StringUtil.isWhitespace(value));
    }

    @ParameterizedTest
    @CsvSource({
            "' '            , '    \r \n \r\n'",
            "' hello there ', '   hello   \r \n  there    \n'",
            "'hello'        , 'hello'",
            "'hello there'  , 'hello\nthere'"
    })
    public void normaliseWhiteSpace(String expected, String value) {
        assertEquals(expected, normaliseWhitespace(value));
    }

    @Test public void normaliseWhiteSpaceHandlesHighSurrogates() {
        String test71540chars = "\ud869\udeb2\u304b\u309a  1";
        String test71540charsExpectedSingleWhitespace = "\ud869\udeb2\u304b\u309a 1";

        assertEquals(test71540charsExpectedSingleWhitespace, normaliseWhitespace(test71540chars));
        String extractedText = Jsoup.parse(test71540chars).text();
        assertEquals(test71540charsExpectedSingleWhitespace, extractedText);
    }

    @ParameterizedTest
    @CsvSource({
            "'http://example.com/one/two?three'     , 'http://example.com'      , './one/two?three'",
            "'http://example.com/one/two?three'     , 'http://example.com?one'  , './one/two?three'",
            "'http://example.com/one/two?three#four', 'http://example.com'      , './one/two?three#four'",
            "'https://example.com/one'              , 'http://example.com/'     , 'https://example.com/one'",
            "'http://example.com/one/two.html'      , 'http://example.com/two/' , '../one/two.html'",
            "'https://example2.com/one'             , 'https://example.com/'    , '//example2.com/one'",
            "'https://example.com:8080/one'         , 'https://example.com:8080', './one'",
            "'https://example2.com/one'             , 'http://example.com/'     , 'https://example2.com/one'",
            "'https://example.com/one'              , 'wrong'                   , 'https://example.com/one'",
            "'https://example.com/one'              , 'https://example.com/one' , ''",
            "''                                     , 'wrong'                   , 'also wrong'",
            "'ftp://example.com/one'                , 'ftp://example.com/two/'  , '../one'",
            "'ftp://example.com/one/two.c'          , 'ftp://example.com/one/'  , './two.c'",
            "'ftp://example.com/one/two.c'          , 'ftp://example.com/one/'  , 'two.c'"
    })
    public void resolvesRelativeUrls(String expected, String baseUrl, String relUrl) {
        assertEquals(expected, resolve(baseUrl, relUrl));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"example.com", "One Two"})
    void isAscii_validValues(String value) {
        assertTrue(StringUtil.isAscii(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"🧔", "测试", "测试.com"})
    void isAscii_invalidValues(String value) {
        assertFalse(StringUtil.isAscii(value));
    }
}
