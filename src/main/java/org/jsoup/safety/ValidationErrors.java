package org.jsoup.safety;

import java.util.*;

public class ValidationErrors {
    private final List<ValidationError> errors;

    public ValidationErrors() {
        this.errors = new ArrayList<ValidationError>();
    }

    public ValidationErrors addTag(String tag, String html) {
        errors.add(new ValidationError(tag, null, html));

        return this;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public ValidationErrors addAttribute(String tag, String attribute, String html) {
        errors.add(new ValidationError(tag, attribute, html));

        return this;
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    @Override public String toString() {
        StringBuilder stringBuilder = new StringBuilder("[");

        for (ValidationError error : errors) {
            stringBuilder.append(error.toString()).append(", ");
        }

        return (errors.isEmpty() ? "[" : stringBuilder.substring(0, stringBuilder.length() - 2)) + "]";
    }
}
