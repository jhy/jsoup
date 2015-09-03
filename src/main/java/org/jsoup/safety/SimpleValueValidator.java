package org.jsoup.safety;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

public final class SimpleValueValidator extends BaseValueValidator {

    private final boolean ignoreCase;

    private final Set<String> allowedValues = new HashSet<String>();

    public SimpleValueValidator(String... allowedValues) {

        this(false, allowedValues);
    }

    public SimpleValueValidator(boolean ignoreCase, String... allowedValues) {
        this(ignoreCase, false, allowedValues);
    }

    public SimpleValueValidator(boolean ignoreCase, boolean required, String... allowedValues) {
        super(required);
        this.ignoreCase = ignoreCase;
        if (ignoreCase) {
            for (String allowedValue : allowedValues) {
                this.allowedValues.add(allowedValue.toUpperCase());
            }
        } else {
            this.allowedValues.addAll(Arrays.asList(allowedValues));
        }
    }

    public boolean isSafe(Element el, Attribute attr) {
        String value = attr.getValue();
        return allowedValues.contains(ignoreCase ? value.toUpperCase() : value);
    }
}
