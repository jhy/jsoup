package org.jsoup.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidateTest {
    @Test
    public void testNotNull() {
        Validate.notNull("foo");
        boolean threw = false;
        try {
            Validate.notNull(null);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        Assertions.assertTrue(threw);
    }

    @Test void stacktraceFiltersOutValidateClass() {
        boolean threw = false;
        try {
            Validate.notNull(null);
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Object must not be null", e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement trace : stackTrace) {
                assertNotEquals(trace.getClassName(), Validate.class.getName());
            }
            assertTrue(stackTrace.length >= 1);
        }
        Assertions.assertTrue(threw);
    }

    @Test void nonnullParam() {
        boolean threw = true;
        try {
            Validate.notNullParam(null, "foo");
        } catch (ValidationException e) {
            assertEquals("The parameter 'foo' must not be null.", e.getMessage());
        }
        assertTrue(threw);
    }
    @Test
    void testEnsureNotNullWithNonNullObject() {
        Object nonNullObject = new Object();
        assertEquals(nonNullObject, Validate.ensureNotNull(nonNullObject));
    }

    @Test
    void testEnsureNotNullWithNullObject() {
        Object nullObject = null;
        assertThrows(ValidationException.class,() -> Validate.ensureNotNull(nullObject));
    }

    @Test
    void testIsTrueWithFalseValue() {
        assertThrows(ValidationException.class,() -> Validate.isTrue(false));
    }

    @Test
    void testIsFalseWithTrueValue() {
        assertThrows(ValidationException.class,() -> Validate.isFalse(true));
    }

    @Test
    void testNoNullElementsWithNullElement() {
        Object[] objects = { "notNull", null, "anotherNotNull" };
        assertThrows(ValidationException.class, () -> Validate.noNullElements(objects, "Array must not contain null elements"));
    }

    @Test
    void testSomeConditionThatShouldNotBeMet() {
        assertThrows(ValidationException.class, () -> Validate.assertFail("This condition should not be met"));
    }
}
