package org.jsoup.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
