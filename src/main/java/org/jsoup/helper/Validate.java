package org.jsoup.helper;

import javax.annotation.Nullable;

/**
 * Validators to check that method arguments meet expectations. 
 */
public final class Validate {
    
    private Validate() {}

    /**
     * Validates that the object is not null
     * @param obj object to test
     * @throws ValidationException if the object is null
     */
    public static void notNull(@Nullable Object obj) {
        if (obj == null)
            throw new ValidationException("Object must not be null");
    }

    /**
     Validates that the parameter is not null

     * @param obj the parameter to test
     * @param param the name of the parameter, for presentation in the validation exception.
     * @throws ValidationException if the object is null
     */
    public static void notNullParam(@Nullable final Object obj, final String param) {
        if (obj == null)
            throw new ValidationException(String.format("The parameter '%s' must not be null.", param));
    }

    /**
     * Validates that the object is not null
     * @param obj object to test
     * @param msg message to include in the Exception if validation fails
     * @throws ValidationException if the object is null
     */
    public static void notNull(@Nullable Object obj, String msg) {
        if (obj == null)
            throw new ValidationException(msg);
    }

    /**
     Verifies the input object is not null, and returns that object. Effectively this casts a nullable object to a non-
     null object. (Works around lack of Objects.requestNonNull in Android version.)
     * @param obj nullable object to case to not-null
     * @return the object, or throws an exception if it is null
     * @throws ValidationException if the object is null
     */
    public static Object ensureNotNull(@Nullable Object obj) {
        if (obj == null)
            throw new ValidationException("Object must not be null");
        else return obj;
    }

    /**
     Verifies the input object is not null, and returns that object. Effectively this casts a nullable object to a non-
     null object. (Works around lack of Objects.requestNonNull in Android version.)
     * @param obj nullable object to case to not-null
     * @param msg the String format message to include in the validation exception when thrown
     * @param args the arguments to the msg
     * @return the object, or throws an exception if it is null
     * @throws ValidationException if the object is null
     */
    public static Object ensureNotNull(@Nullable Object obj, String msg, Object... args) {
        if (obj == null)
            throw new ValidationException(String.format(msg, args));
        else return obj;
    }

    /**
     * Validates that the value is true
     * @param val object to test
     * @throws ValidationException if the object is not true
     */
    public static void isTrue(boolean val) {
        if (!val)
            throw new ValidationException("Must be true");
    }

    /**
     * Validates that the value is true
     * @param val object to test
     * @param msg message to include in the Exception if validation fails
     * @throws ValidationException if the object is not true
     */
    public static void isTrue(boolean val, String msg) {
        if (!val)
            throw new ValidationException(msg);
    }

    /**
     * Validates that the value is false
     * @param val object to test
     * @throws ValidationException if the object is not false
     */
    public static void isFalse(boolean val) {
        if (val)
            throw new ValidationException("Must be false");
    }

    /**
     * Validates that the value is false
     * @param val object to test
     * @param msg message to include in the Exception if validation fails
     * @throws ValidationException if the object is not false
     */
    public static void isFalse(boolean val, String msg) {
        if (val)
            throw new ValidationException(msg);
    }

    /**
     * Validates that the array contains no null elements
     * @param objects the array to test
     * @throws ValidationException if the array contains a null element
     */
    public static void noNullElements(Object[] objects) {
        noNullElements(objects, "Array must not contain any null objects");
    }

    /**
     * Validates that the array contains no null elements
     * @param objects the array to test
     * @param msg message to include in the Exception if validation fails
     * @throws ValidationException if the array contains a null element
     */
    public static void noNullElements(Object[] objects, String msg) {
        for (Object obj : objects)
            if (obj == null)
                throw new ValidationException(msg);
    }

    /**
     * Validates that the string is not null and is not empty
     * @param string the string to test
     * @throws ValidationException if the string is null or empty
     */
    public static void notEmpty(@Nullable String string) {
        if (string == null || string.length() == 0)
            throw new ValidationException("String must not be empty");
    }

    /**
     Validates that the string parameter is not null and is not empty
     * @param string the string to test
     * @param param the name of the parameter, for presentation in the validation exception.
     * @throws ValidationException if the string is null or empty
     */
    public static void notEmptyParam(@Nullable final String string, final String param) {
        if (string == null || string.length() == 0)
            throw new ValidationException(String.format("The '%s' parameter must not be empty.", param));
    }

    /**
     * Validates that the string is not null and is not empty
     * @param string the string to test
     * @param msg message to include in the Exception if validation fails
     * @throws ValidationException if the string is null or empty
     */
    public static void notEmpty(@Nullable String string, String msg) {
        if (string == null || string.length() == 0)
            throw new ValidationException(msg);
    }

    /**
     * Blow up if we reach an unexpected state.
     * @param msg message to think about
     * @throws IllegalStateException if we reach this state
     */
    public static void wtf(String msg) {
        throw new IllegalStateException(msg);
    }

    /**
     Cause a failure.
     @param msg message to output.
     @throws IllegalStateException if we reach this state
     */
    public static void fail(String msg) {
        throw new ValidationException(msg);
    }

    /**
     Cause a failure.
     @param msg message to output.
     @param args the format arguments to the msg
     @throws IllegalStateException if we reach this state
     */
    public static void fail(String msg, Object... args) {
        throw new ValidationException(String.format(msg, args));
    }
}
