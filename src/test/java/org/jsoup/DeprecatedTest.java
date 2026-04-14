package org.jsoup;

import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.jsoup.internal.StringUtil;
import org.jsoup.internal.Normalizer;
import org.jsoup.internal.Functions;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/*
*  Includes simple tests for deprecated methods (for BACKWARDS compatibility)
* - Lucas
*/
public class DeprecatedTest {
    @Test
    void normalizeLowercaseTest() {
        String input = "  Hello WORLD  ";

        // When true, it should NOT trim, only lowercase
        String result = Normalizer.normalize(input, true);

        assertEquals("  hello world  ", result, "Should lowercase but keep spaces.");
    }

    @Test
    void testFunctions() {
        Function<String, List<Integer>> function1 = Functions.listFunction();
        Function<String, Set<Integer>> function2 = Functions.setFunction();
        Function<String, Map<String, String>> function3 = Functions.mapFunction();
        Function<String, IdentityHashMap<String, String>> function4 = Functions.identityMapFunction();
        assertNotNull(function1);
        assertNotNull(function2);
        assertNotNull(function3);
        assertNotNull(function4);
        assertNotNull(function1.apply("LUCAS"));
        assertNotNull(function2.apply("LUCAS"));
        assertNotNull(function3.apply("LUCAS"));
        assertNotNull(function4.apply("LUCAS"));
    }
}
