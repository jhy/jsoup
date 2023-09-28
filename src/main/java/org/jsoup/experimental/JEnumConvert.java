package org.jsoup.experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signals that an Enum field should be convertible from some String
 * value (pulled from XML).
 * 
 * Essentially the same as {@link JConverter}, but with a default way of
 * converting.
 * @author Sam Hieken
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JEnumConvert {
	/**
	 * The converter. If an array is passed, all values besides the first
	 * will be ignored.
	 * 
	 * If an empty array is passed (the same as passing nothing), the field type,
//	 * which is assumed to be an Enum, will have its {@code valueOf(String)} method called.
	 */
	Class<? extends JConverter<?>>[] value() default {};
}