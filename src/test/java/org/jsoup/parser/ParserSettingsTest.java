package org.jsoup.parser;

import org.jsoup.MultiLocaleExtension.MultiLocale;
import org.jsoup.nodes.Attributes;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserSettingsTest {
    @ParameterizedTest
    @MultiLocale
    public void caseSupport(Locale locale) {
        Locale.setDefault(locale);

        ParseSettings bothOn = new ParseSettings(true, true);
        ParseSettings bothOff = new ParseSettings(false, false);
        ParseSettings tagOn = new ParseSettings(true, false);
        ParseSettings attrOn = new ParseSettings(false, true);

        assertEquals("IMG", bothOn.normalizeTag("IMG"));
        assertEquals("ID", bothOn.normalizeAttribute("ID"));

        assertEquals("img", bothOff.normalizeTag("IMG"));
        assertEquals("id", bothOff.normalizeAttribute("ID"));

        assertEquals("IMG", tagOn.normalizeTag("IMG"));
        assertEquals("id", tagOn.normalizeAttribute("ID"));

        assertEquals("img", attrOn.normalizeTag("IMG"));
        assertEquals("ID", attrOn.normalizeAttribute("ID"));
    }

    @ParameterizedTest
    @MultiLocale
    public void attributeCaseNormalization(Locale locale) {
        Locale.setDefault(locale);

        ParseSettings parseSettings = new ParseSettings(false, false);
        String normalizedAttribute = parseSettings.normalizeAttribute("HIDDEN");

        assertEquals("hidden", normalizedAttribute);
    }

    @ParameterizedTest
    @MultiLocale
    public void attributesCaseNormalization(Locale locale) {
        Locale.setDefault(locale);

        ParseSettings parseSettings = new ParseSettings(false, false);
        Attributes attributes = new Attributes();
        attributes.put("ITEM", "1");

        Attributes normalizedAttributes = parseSettings.normalizeAttributes(attributes);

        assertEquals("item", normalizedAttributes.asList().get(0).getKey());
    }
}
