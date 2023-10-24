package org.jsoup.helper;

import java.util.ArrayList;
import java.util.List;

/**
 Validation exceptions, as thrown by the methods in {@link Validate}.
 */
public class ValidationException extends IllegalArgumentException {

    public static final String Validator = Validate.class.getName();

    public ValidationException(String msg) {
        super(msg);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // Filters out the Validate class from the stacktrace, to more clearly point at the root-cause.

        super.fillInStackTrace();

        StackTraceElement[] stackTrace = getStackTrace();
        List<StackTraceElement> filteredTrace = new ArrayList<>();
        for (StackTraceElement trace : stackTrace) {
            if (trace.getClassName().equals(Validator)) continue;
            filteredTrace.add(trace);
        }

        setStackTrace(filteredTrace.toArray(new StackTraceElement[0]));

        return this;
    }
}
