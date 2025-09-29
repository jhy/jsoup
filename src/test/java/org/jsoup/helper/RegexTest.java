package org.jsoup.helper;

import org.jsoup.select.QueryParser;
import org.jsoup.select.Selector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTest {

    private boolean originalUseRe2j; // track original setting

    @BeforeEach
    void setUp() {
        originalUseRe2j = Regex.wantsRe2j();
    }

    @AfterEach
    void tearDown() {
        Regex.wantsRe2j(originalUseRe2j); // restore original setting
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testRegexDelegates(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        assertEquals(Regex.usingRe2j(), useRe2j);
        String pattern = "(\\d+)";
        String input = "12345";

        Regex regex = Regex.compile(pattern);
        Regex.Matcher matcher = regex.matcher(input);
        assertTrue(matcher.find());
    }

    @Test
    void jdkSupportsBackreferenceMatches() {
        Regex.wantsRe2j(false);
        String pattern = "(\\w+)\\s+\\1"; // backreference to group 1
        String input = "hello hello";

        Regex regex = Regex.compile(pattern);
        Regex.Matcher matcher = regex.matcher(input);
        assertTrue(matcher.find());
    }

    @Test
    void re2jRejectsBackreferenceThrows() {
        Regex.wantsRe2j(true);
        String pattern = "(\\w+)\\s+\\1"; // backreference unsupported by RE2J

        assertThrows(ValidationException.class, () -> Regex.compile(pattern));
        // and not the rej2 PatternSyntaxException
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void queryParserThrowsSelectorExceptionOnMalformedRegex(boolean useRe2j) {
        Regex.wantsRe2j(useRe2j);
        String query = "[attr~=(unclosed]";

        boolean threw = false;
        try {
            QueryParser.parse(query);
        } catch (Selector.SelectorParseException e) {
            threw = true;
            assertTrue(e.getMessage().contains("Pattern syntax error"));
        }
        assertTrue(threw);
    }
}
