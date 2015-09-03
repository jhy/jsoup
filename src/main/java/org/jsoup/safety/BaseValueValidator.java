package org.jsoup.safety;

public abstract class BaseValueValidator implements ValueValidator {

    private final boolean required;

    protected BaseValueValidator(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }
}