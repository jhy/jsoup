package org.jsoup.parser;


import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HtmlTreeBuilderTest {
    @Test
    public void ensureSearchArraysAreSorted() {
        List<Object[]> constants = HtmlTreeBuilderStateTest.findConstantArrays(HtmlTreeBuilder.class);
        HtmlTreeBuilderStateTest.ensureSorted(constants);
        assertEquals(8, constants.size());
    }

    @Test
    public void nonnull() {
        assertThrows(IllegalArgumentException.class, () -> {
                HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
                treeBuilder.parse(null, null, null); // not sure how to test that these visual warnings actually appear! - test below checks for method annotation
            }
        ); // I'm not convinced that this lambda is easier to read than the old Junit 4 @Test(expected=IEA.class)...
    }

    @Test public void nonnullAssertions() throws NoSuchMethodException {
        Method parseMethod = TreeBuilder.class.getDeclaredMethod("parse", Reader.class, String.class, Parser.class);
        assertNotNull(parseMethod);
        Annotation[] declaredAnnotations = parseMethod.getDeclaredAnnotations();
        boolean seen = false;
        for (Annotation annotation : declaredAnnotations) {
            if (annotation.annotationType().isAssignableFrom(ParametersAreNonnullByDefault.class))
                seen = true;
        }

        // would need to rework this if/when that annotation moves from the method to the class / package.
        assertTrue(seen);

    }
}
