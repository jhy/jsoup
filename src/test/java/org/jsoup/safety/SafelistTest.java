package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SafelistTest {
    private static final String TEST_TAG = "testTag";
    private static final String TEST_ATTRIBUTE = "testAttribute";
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
    public void testDataAttributes_forSingleTag() {
        Safelist safelist1 = Safelist.none()
                .addDataAttributes(TEST_TAG);
        Safelist safelist2 = Safelist.none()
                .addDataAttributes(TEST_TAG, "customData-");
        Attributes attributes = new Attributes();
        Attribute attr1 = new Attribute("data-test1", "data value 1");
        Attribute attr2 = new Attribute("data-test2", "data value 2");
        Attribute attr3 = new Attribute("customData-test3", "data value 3");
        attributes.put(attr1);
        attributes.put(attr2);
        attributes.put(attr3);
        Element elem1 = new Element(Tag.valueOf(TEST_TAG), "", attributes);
        Element elem2 = new Element(Tag.valueOf("div"), "", attributes);

        assertTrue(safelist1.isSafeAttribute(TEST_TAG, elem1, attr1));
        assertTrue(safelist1.isSafeAttribute(TEST_TAG, elem1, attr2));
        assertFalse(safelist1.isSafeAttribute(TEST_TAG, elem1, attr3));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, elem1, attr3));
        assertFalse(safelist1.isSafeAttribute("div", elem2, attr1));
        assertFalse(safelist1.isSafeAttribute("div", elem2, attr2));
        assertFalse(safelist1.isSafeAttribute("div", elem2, attr3));
    }

    @Test
    public void testDataAttributes_forAll() {
        Safelist safelist1 = Safelist.none()
                .addDataAttributes(Safelist.TAG_ALL);
        Safelist safelist2 = Safelist.none()
                .addDataAttributes(Safelist.TAG_ALL, "customData-");
        Attributes attributes = new Attributes();
        Attribute attr1 = new Attribute("data-test1", "data value 1");
        Attribute attr2 = new Attribute("data-test2", "data value 2");
        Attribute attr3 = new Attribute("customData-test3", "data value 3");
        attributes.put(attr1);
        attributes.put(attr2);
        attributes.put(attr3);
        Element elem1 = new Element(Tag.valueOf(TEST_TAG), "", attributes);
        Element elem2 = new Element(Tag.valueOf("div"), "", attributes);

        assertTrue(safelist1.isSafeAttribute(TEST_TAG, elem1, attr1));
        assertTrue(safelist1.isSafeAttribute(TEST_TAG, elem1, attr2));
        assertFalse(safelist1.isSafeAttribute(TEST_TAG, elem1, attr3));
        assertTrue(safelist2.isSafeAttribute(TEST_TAG, elem1, attr3));
        assertTrue(safelist1.isSafeAttribute("div", elem2, attr1));
        assertTrue(safelist1.isSafeAttribute("div", elem2, attr2));
        assertFalse(safelist1.isSafeAttribute("div", elem2, attr3));
        assertTrue(safelist2.isSafeAttribute("div", elem2, attr3));
    }

}
