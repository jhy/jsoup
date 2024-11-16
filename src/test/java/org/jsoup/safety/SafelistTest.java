package org.jsoup.safety;

import org.jsoup.helper.ValidationException;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SafelistTest {
    private static final String TEST_TAG = "testTag";
    private static final String TEST_ATTRIBUTE = "testAttribute";
    private static final String TEST_DATA_ATTRIBUTE = "data-" + TEST_ATTRIBUTE;
    private static final String TEST_SCHEME = "valid-scheme";
    private static final String TEST_VALUE = TEST_SCHEME + "://testValue";

    @Test
    public void testCopyConstructor_noSideEffectOnTags() {
        Safelist safelist1 = Safelist.none().addTags(TEST_TAG);
        Safelist safelist2 = new Safelist(safelist1);
        safelist1.addTags("invalidTag");

        assertFalse(safelist2.isSafeTag("invalidTag"));
    }

    @Test
    public void testCopyConstructor_noSideEffectOnAttributes() {
        Safelist safelist1 = Safelist.none().addAttributes(TEST_TAG, TEST_ATTRIBUTE);
        Safelist safelist2 = new Safelist(safelist1);
        safelist1.addAttributes(TEST_TAG, "invalidAttribute");

        assertFalse(safelist2.isSafeAttribute(TEST_TAG, null, new Attribute("invalidAttribute", TEST_VALUE)));
    }

    @Test
    public void testCopyConstructor_noSideEffectOnEnforcedAttributes() {
        Safelist safelist1 = Safelist.none().addEnforcedAttribute(TEST_TAG, TEST_ATTRIBUTE, TEST_VALUE);
        Safelist safelist2 = new Safelist(safelist1);
        safelist1.addEnforcedAttribute(TEST_TAG, TEST_ATTRIBUTE, "invalidValue");

        for (Attribute enforcedAttribute : safelist2.getEnforcedAttributes(TEST_TAG)) {
            assertNotEquals("invalidValue", enforcedAttribute.getValue());
        }
    }

    @Test
    public void testCopyConstructor_noSideEffectOnProtocols() {
        final String invalidScheme = "invalid-scheme";
        Safelist safelist1 = Safelist.none()
                .addAttributes(TEST_TAG, TEST_ATTRIBUTE)
                .addProtocols(TEST_TAG, TEST_ATTRIBUTE, TEST_SCHEME);
        Safelist safelist2 = new Safelist(safelist1);
        safelist1.addProtocols(TEST_TAG, TEST_ATTRIBUTE, invalidScheme);

        Attributes attributes = new Attributes();
        Attribute invalidAttribute = new Attribute(TEST_ATTRIBUTE, invalidScheme + "://someValue");
        attributes.put(invalidAttribute);
        Element invalidElement = new Element(Tag.valueOf(TEST_TAG), "", attributes);

        assertFalse(safelist2.isSafeAttribute(TEST_TAG, invalidElement, invalidAttribute));
    }

    @Test
    void noscriptIsBlocked() {
        boolean threw = false;
        Safelist safelist = null;
        try {
            safelist = Safelist.none().addTags("NOSCRIPT");
        } catch (ValidationException validationException) {
            threw = true;
            assertTrue(validationException.getMessage().contains("unsupported"));
        }
        assertTrue(threw);
        assertNull(safelist);
    }

    @Test
    public void testAttributeWildcard() {
        Safelist safelist1 = Safelist.none();
        Safelist safelist2 = new Safelist(safelist1).addWildcardAttributes(TEST_TAG, "data-.+");
        Attribute attr = new Attribute(TEST_DATA_ATTRIBUTE, TEST_VALUE);

        assertFalse(safelist1.isSafeAttribute(TEST_TAG, null, attr));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, null, attr));
        assertFalse(safelist1.isSafeAttribute(TEST_TAG + "1", null, attr));
    }

    @Test
    public void test8GlobalAttributeWildcard() {
        Safelist safelist1 = Safelist.none();
        Safelist safelist2 = new Safelist(safelist1).addWildcardGlobalAttributes("data-.+");
        Attribute attr = new Attribute(TEST_DATA_ATTRIBUTE, TEST_VALUE);

        assertFalse(safelist1.isSafeAttribute(TEST_TAG, null, attr));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, null, attr));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG + "1", null, attr));
    }
}
