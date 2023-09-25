package org.jsoup.experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jsoup.nodes.Element;

/**
 * Indicates that a field should be parsed from an HTML element, which will be found
 * using the specified CSS selector.
 * <br><br>
 * On primitives (and their wrappers), date objects, and strings, it will
 * simply get the {@link Element#text()} of the specified selector's first 
 * result and parsing it to the correct type.
 * <br><br>
 * On all other objects (except collections), {@link JsoupConverter#getObjectFromElement(Element, Class)}
 * will be called recursively to parse them using the selector's <b>first</b> result as input.
 * <br><br>
 * On collections, {@link JsoupConverter#getObjectsFromElements(java.util.Collection, Class)}
 * will be called recursively to parse them using <b>ALL</b> the selector's results as input.
 * 
 * @author Sam Hieken
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JSelector {
	String value() default "";
}
