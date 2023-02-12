package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SafelistTest {
    private static final String TEST_TAG = "testTag";
    private static final String TEST_TAG2 = "testTag2";
    private static final String TEST_ATTRIBUTE = "testAttribute-*";
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
    public void testIsSafeAttribute() {
        String[] attrs = {"-*", "data-", "data-*", "custom-data-*"};
        Safelist safelist = Safelist.none()
                .addAttributes(TEST_TAG, attrs);
        Safelist safelist2 = Safelist.none()
                .addAttributes(":all", attrs);
        Attributes attributes = new Attributes();
        Attribute attr1 = new Attribute("-*", "data value 1");
        Attribute attr2 = new Attribute("data-", "data value 2");
        Attribute attr3 = new Attribute("data-test3", "data value 3");
        Attribute attr4 = new Attribute("data-test4", "data value 4");
        Attribute attr5 = new Attribute("custom-data-test5", "data value 5");
        attributes.put(attr1).put(attr2).put(attr3).put(attr4).put(attr5);
        Element elem1 = new Element(Tag.valueOf(TEST_TAG), "", attributes);
        Element elem2 = new Element(Tag.valueOf(TEST_TAG2), "", attributes);

        assertTrue(safelist.isSafeAttribute(TEST_TAG, elem1, attr1));
        assertTrue(safelist.isSafeAttribute(TEST_TAG, elem1, attr2));
        assertTrue(safelist.isSafeAttribute(TEST_TAG, elem1, attr3));
        assertTrue(safelist.isSafeAttribute(TEST_TAG, elem1, attr4));
        assertTrue(safelist.isSafeAttribute(TEST_TAG, elem1, attr5));

        assertFalse(safelist.isSafeAttribute(TEST_TAG2, elem2, attr1));
        assertFalse(safelist.isSafeAttribute(TEST_TAG2, elem2, attr2));
        assertFalse(safelist.isSafeAttribute(TEST_TAG2, elem2, attr3));
        assertFalse(safelist.isSafeAttribute(TEST_TAG2, elem2, attr4));
        assertFalse(safelist.isSafeAttribute(TEST_TAG2, elem2, attr5));

        assertTrue(safelist2.isSafeAttribute(TEST_TAG, elem1, attr1));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, elem1, attr2));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, elem1, attr3));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, elem1, attr4));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, elem1, attr5));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG2, elem2, attr1));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG2, elem2, attr2));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG2, elem2, attr3));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG2, elem2, attr4));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG2, elem2, attr5));
    }

}
