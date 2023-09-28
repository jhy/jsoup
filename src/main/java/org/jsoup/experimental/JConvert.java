package org.jsoup.experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signals that a field should have a custom way to be convertible from some String
 * value (pulled from XML).
 * @author Sam Hieken
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JConvert {
	/**
	 * The converter.
	 */
	Class<? extends JConverter<?>> value();
}