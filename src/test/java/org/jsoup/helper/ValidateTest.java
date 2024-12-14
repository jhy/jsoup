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

    @Test
    void stacktraceFiltersOutValidateClass() {
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

    @Test
    void nonnullParam() {
        boolean threw = true;
        try {
            Validate.notNullParam(null, "foo");
        } catch (ValidationException e) {
            assertEquals("The parameter 'foo' must not be null.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testWtf() {
        boolean threw = false;
        try {
            Validate.wtf("Unexpected state reached");
        } catch (IllegalStateException e) {
            threw = true;
            assertEquals("Unexpected state reached", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testEnsureNotNull() {
        // Test with a non-null object
        Object obj = new Object();
        assertSame(obj, Validate.ensureNotNull(obj));

        // Test with a null object
        boolean threw = false;
        try {
            Validate.ensureNotNull(null);
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Object must not be null", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testEnsureNotNullWithMessage() {
        // Test with a non-null object
        Object obj = new Object();
        assertSame(obj, Validate.ensureNotNull(obj, "Object must not be null"));

        // Test with a null object
        boolean threw = false;
        try {
            Validate.ensureNotNull(null, "Custom error message");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Custom error message", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testEnsureNotNullWithFormattedMessage() {
        // Test with a non-null object
        Object obj = new Object();
        assertSame(obj, Validate.ensureNotNull(obj, "Object must not be null: %s", "additional info"));

        // Test with a null object
        boolean threw = false;
        try {
            Validate.ensureNotNull(null, "Object must not be null: %s", "additional info");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Object must not be null: additional info", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testNotNullParam() {
        // Test with a non-null object
        Object obj = new Object();
        Validate.notNullParam(obj, "param");

        // Test with a null object
        boolean threw = false;
        try {
            Validate.notNullParam(null, "param");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("The parameter 'param' must not be null.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testNotEmpty() {
        // Test with a non-empty string
        String str = "foo";
        Validate.notEmpty(str);

        // Test with an empty string
        boolean threw = false;
        try {
            Validate.notEmpty("");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("String must not be empty", e.getMessage());
        }
        assertTrue(threw);

        // Test with a null string
        threw = false;
        try {
            Validate.notEmpty(null);
        } catch (ValidationException e) {
            threw = true;
            assertEquals("String must not be empty", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testIsTrue() {
        // Test with a true value
        Validate.isTrue(true);

        // Test with a false value
        boolean threw = false;
        try {
            Validate.isTrue(false);
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Must be true", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testIsFalse() {
        // Test with a false value
        Validate.isFalse(false);

        // Test with a true value
        boolean threw = false;
        try {
            Validate.isFalse(true);
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Must be false", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testAssertFail() {
        boolean result = false;
        boolean threw = false;
        try {
            result = Validate.assertFail("This should fail");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("This should fail", e.getMessage());
        }
        assertTrue(threw);
        assertFalse(result);
    }

    @Test
    public void testNotEmptyParam() {
        // Test with a non-empty string
        Validate.notEmptyParam("foo", "param");

        // Test with an empty string
        boolean threw = false;
        try {
            Validate.notEmptyParam("", "param");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("The 'param' parameter must not be empty.", e.getMessage());
        }
        assertTrue(threw);

        // Test with a null string
        threw = false;
        try {
            Validate.notEmptyParam(null, "param");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("The 'param' parameter must not be empty.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testNoNullElementsWithMessage() {
        // Test with an array with no null elements
        Object[] array = {new Object(), new Object()};
        Validate.noNullElements(array, "Custom error message");

        // Test with an array containing a null element
        boolean threw = false;
        try {
            Validate.noNullElements(new Object[]{new Object(), null}, "Custom error message");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Custom error message", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test
    public void testNotEmptyWithMessage() {
        // Test with a non-empty string
        Validate.notEmpty("foo", "Custom error message");

        // Test with an empty string
        boolean threw = false;
        try {
            Validate.notEmpty("", "Custom error message");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Custom error message", e.getMessage());
        }
        assertTrue(threw);

        // Test with a null string
        threw = false;
        try {
            Validate.notEmpty(null, "Custom error message");
        } catch (ValidationException e) {
            threw = true;
            assertEquals("Custom error message", e.getMessage());
        }
        assertTrue(threw);
    }

}
