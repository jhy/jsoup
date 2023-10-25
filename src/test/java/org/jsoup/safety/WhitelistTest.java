package org.jsoup.safety;

import org.jsoup.helper.ValidationException;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WhitelistTest {
    private static final String TEST_TAG = "testTag";
    private static final String TEST_ATTRIBUTE = "testAttribute";
    private static final String TEST_SCHEME = "valid-scheme";
    private static final String TEST_VALUE = TEST_SCHEME + "://testValue";

    @Test
    public void testCopyConstructor_noSideEffectOnTags() {
        Whitelist whitelist1 = Whitelist.none().addTags(TEST_TAG);
        Whitelist whitelist2 = new Whitelist(whitelist1);
        whitelist1.addTags("invalidTag");

        assertFalse(whitelist2.isSafeTag("invalidTag"));
    }

    @Test
    public void testCopyConstructor_noSideEffectOnAttributes() {
        Whitelist whitelist1 = Whitelist.none().addAttributes(TEST_TAG, TEST_ATTRIBUTE);
        Whitelist whitelist2 = new Whitelist(whitelist1);
        whitelist1.addAttributes(TEST_TAG, "invalidAttribute");

        assertFalse(whitelist2.isSafeAttribute(TEST_TAG, null, new Attribute("invalidAttribute", TEST_VALUE)));
    }

    @Test
    public void testCopyConstructor_noSideEffectOnEnforcedAttributes() {
        Whitelist whitelist1 = Whitelist.none().addEnforcedAttribute(TEST_TAG, TEST_ATTRIBUTE, TEST_VALUE);
        Whitelist whitelist2 = new Whitelist(whitelist1);
        whitelist1.addEnforcedAttribute(TEST_TAG, TEST_ATTRIBUTE, "invalidValue");

        for (Attribute enforcedAttribute : whitelist2.getEnforcedAttributes(TEST_TAG)) {
            assertNotEquals("invalidValue", enforcedAttribute.getValue());
        }
    }

    @Test
    public void testCopyConstructor_noSideEffectOnProtocols() {
        final String invalidScheme = "invalid-scheme";
        Whitelist whitelist1 = Whitelist.none()
                .addAttributes(TEST_TAG, TEST_ATTRIBUTE)
                .addProtocols(TEST_TAG, TEST_ATTRIBUTE, TEST_SCHEME);
        Whitelist whitelist2 = new Whitelist(whitelist1);
        whitelist1.addProtocols(TEST_TAG, TEST_ATTRIBUTE, invalidScheme);

        Attributes attributes = new Attributes();
        Attribute invalidAttribute = new Attribute(TEST_ATTRIBUTE, invalidScheme + "://someValue");
        attributes.put(invalidAttribute);
        Element invalidElement = new Element(Tag.valueOf(TEST_TAG), "", attributes);

        assertFalse(whitelist2.isSafeAttribute(TEST_TAG, invalidElement, invalidAttribute));
    }

    @Test
    void noscriptIsBlocked() {
        boolean threw = false;
        Whitelist whitelist = null;
        try {
            whitelist = Whitelist.none().addTags("NOSCRIPT");
        } catch (ValidationException validationException) {
            threw = true;
            assertTrue(validationException.getMessage().contains("unsupported"));
        }
        assertTrue(threw);
        assertNull(whitelist);
    }
}
