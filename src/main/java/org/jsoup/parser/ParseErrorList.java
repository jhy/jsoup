package org.jsoup.parser;

import java.util.ArrayList;

/**
 * A container for ParseErrors with error-tracking capabilities.
 * Refactored to separate responsibilities into distinct classes.
 */
public class ParseErrorList extends ArrayList<ParseError> {
    // private static final int INITIAL_CAPACITY = 16; // Define the constant for initial capacity
    private final int initialCapacity;
    private final int maxSize;
    private final ErrorTracker errorTracker; // New class to handle error tracking logic

    ParseErrorList(int initialCapacity, int maxSize) {
        super(initialCapacity);
        this.initialCapacity = initialCapacity;
        this.maxSize = maxSize;
        this.errorTracker = new ErrorTracker(maxSize); // Initialize errorTracker
    }

    /**
     * Create a new ParseErrorList with the same settings, but no errors in the list.
     * @param copy initial and max size details to copy
     */
    ParseErrorList(ParseErrorList copy) {
        this(copy.initialCapacity, copy.maxSize);
    }

    boolean canAddError() {
        return errorTracker.canAddError(); // Delegate to errorTracker
    }

    int getMaxSize() {
        return maxSize;
    }

    public static ParseErrorList noTracking() {
        return new ParseErrorList(0, 0);
    }

    public static ParseErrorList tracking(int maxSize) {
        return new ParseErrorList(maxSize, maxSize);
    }

    @Override
    public Object clone() {
        // all class fields are primitive, so native clone is enough.
        return super.clone();
    }

    // New class to handle error tracking logic
    private static class ErrorTracker {
        private final int maxSize;

        ErrorTracker(int maxSize) {
            this.maxSize = maxSize;
        }

        boolean canAddError() {
            return maxSize > 0; // For now, it's a simple check, but could evolve
        }
    }
}
