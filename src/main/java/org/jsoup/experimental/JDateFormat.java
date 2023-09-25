package org.jsoup.experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

/**
 * Specifies the date format for a field when parsing from XML.
 * 
 * Works on all Java 8 time types, as well as {@link Date}
 * @author Sam
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JDateFormat {
	String value() default "yyyy-MM-dd'T'HH:mm:ss";
}
