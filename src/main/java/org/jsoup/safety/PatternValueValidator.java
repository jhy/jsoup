package org.jsoup.safety;

import java.util.regex.Pattern;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

public final class PatternValueValidator extends BaseValueValidator {

    private final Pattern pattern;

    public PatternValueValidator(String regex) {
        this(regex, false);
    }

    public PatternValueValidator(String pattern, boolean ignoreCase) {
        this(pattern, ignoreCase, false);
    }

    public PatternValueValidator(String pattern, boolean ignoreCase, boolean required) {
        super(required);
        this.pattern = ignoreCase ? Pattern.compile(pattern, Pattern.CASE_INSENSITIVE) : Pattern.compile(pattern);
    }

    public boolean isSafe(Element el, Attribute attr) {
        return pattern.matcher(attr.getValue()).matches();
    }
}
