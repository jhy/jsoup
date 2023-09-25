package org.jsoup.experimental;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Converts XML into Java objects.
 * @author Sam Hieken
 *
 */
public class JsoupConverter {
	public <T> List<T> getObjectsFromElements(Collection<? extends Element> elems, Class<T> type) {
		return (List<T>) getObjectsFromElements(elems, type, false);
	}
	
	private <T> Collection<T> getObjectsFromElements(Collection<? extends Element> elems, Class<T> type, boolean ignoreDuplicates) {
		final Collection<T> ret = ignoreDuplicates 
				? new HashSet<>(elems.size())
				: new ArrayList<>(elems.size());
		
		for (Element elem : elems)
			ret.add(getObjectFromElement(elem, type));
		
		return ret;
	}
	
	public <T> T getObjectFromElement(Element elem, Class<T> type) {
		try {
			return getObjectFromElement0(elem, type);
		} catch (IllegalArgumentException | IllegalAccessException | ParseException | InvocationTargetException | InstantiationException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}

	private <T> T getObjectFromElement0(Element elem, Class<T> type) throws IllegalArgumentException, IllegalAccessException, ParseException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException {
		final T created;
		try {
			// Needs default constructor
			created = type.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			System.err.println("An exception occurred, likely due to a missing default constructor for type '" + type.getName() + "':");
			e.printStackTrace();
			return null;
		}
		
		for (Field field : type.getDeclaredFields()) {
			// Always takes priority over JAttribute (arbitrarily)
			final JSelector js = field.getDeclaredAnnotation(JSelector.class);
			
			if (js != null) {
				String selector = js.value();
				if (selector.equals(""))
					selector = field.getName();
				
				final Element child = elem.selectFirst(selector);
				
				setField(field, created, child == null ? null : child.text(), elem);
				continue;
			}
			
			// JSelector doesn't exist; try JAttribute...
			final JAttribute ja = field.getDeclaredAnnotation(JAttribute.class);
			
			if (ja != null) {
				// If the selector is blank, we use attributes from 
				// the root element.
				final Element child = ja.selector().isBlank() 
						? elem
						: elem.selectFirst(ja.selector());
				
				if (child == null) continue;
				
				String attribute = ja.value();
				
				if (attribute.equals(""))
					attribute = field.getName();
				
				setField(field, created, child.attr(attribute), elem);
			}
		}
		
		return created;
	}
	
	private <T> void setField(Field field, T object, String rawValue, Element root) throws IllegalArgumentException, IllegalAccessException, ParseException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException {
		final Class<?> fieldType = field.getType();
		final JDateFormat dateFormat = field.getDeclaredAnnotation(JDateFormat.class);
		final JEnumConvert enumConverter = field.getDeclaredAnnotation(JEnumConvert.class);
		final JSelector jselector = field.getDeclaredAnnotation(JSelector.class);
		
		field.setAccessible(true);
		
		if (fieldType == String.class)
			field.set(object, rawValue);
		
		// If the field type isn't a String and is empty / null,
		// ignore it.
		else if (rawValue == null || rawValue.isEmpty())
			return;
		
		// Enum
		else if (Enum.class.isAssignableFrom(fieldType)) {
			// Ignore this Enum if no converter annotation was specified.
			// ...may want to remove and go w/ default?
			if (enumConverter == null)
				return;
			
			final Class<? extends JEnumConverter<?>>[] converterArray = enumConverter.value();
			final Method converter = converterArray.length == 0
					? fieldType.getMethod("valueOf", String.class)
					: converterArray[0].getMethod("getEnum", String.class);
			
			// If there's no converter, we use valueOf, which is static; thus the target
			// is null.
			final Object target = converterArray.length == 0
					? null
					: converterArray[0].getConstructor().newInstance();
			
			field.set(object, converter.invoke(target, rawValue));
		}
		
		// - Dates
		else if (fieldType == Date.class) {
			if (dateFormat == null)
				throw new IllegalArgumentException("'java.util.Date' must be accompanied by a @JDateFormat");

			field.set(object, new SimpleDateFormat(dateFormat.value()).parse(rawValue));
		}

		else if (fieldType == LocalDate.class 
				|| fieldType == OffsetDateTime.class
				|| fieldType == ZonedDateTime.class
				|| fieldType == LocalDateTime.class)
			setFieldDate(field, object, rawValue, dateFormat);
		
		else if (fieldType == Boolean.class || fieldType == boolean.class)
			field.set(object, Boolean.parseBoolean(rawValue));
		
		// - Fixed
		else if (fieldType == Byte.class || fieldType == byte.class)
			field.set(object, Byte.parseByte(rawValue));

		else if (fieldType == Short.class || fieldType == short.class)
			field.set(object, Short.parseShort(rawValue));
		
		else if (fieldType == Integer.class || fieldType == int.class)
			field.set(object, Integer.parseInt(rawValue));
		
		else if (fieldType == Long.class || fieldType == long.class)
			field.set(object, Long.parseLong(rawValue));
		
		// - Floating
		else if (fieldType == Float.class || fieldType == float.class)
			field.set(object, Float.parseFloat(rawValue));
		
		else if (fieldType == Double.class || fieldType == double.class)
			field.set(object, Double.parseDouble(rawValue));
		
		// Is this field a Collection?
		// We only check for Lists and Sets since they have Collectors methods.
		else if (fieldType == List.class || fieldType == Set.class) {
			if (jselector == null)
				throw new IllegalArgumentException("Field '" + field.getName() + "' is a collection, and can't be assigned by a @JAttribute");
			
			final String selector = jselector.value().isBlank()
					? field.getName()
					: jselector.value();

			final Elements elems = root.select(selector);
			final Type collectionType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
			
			// Recursively call on each child object.
			field.set(object, getObjectsFromElements(elems, (Class<T>) collectionType, fieldType == Set.class));
		}
		
		// This is likely just a POJO - we will treat it as such.
		else {
			if (jselector == null)
				throw new IllegalArgumentException("Field '" + field.getName() + "' is not a primitive, and can't be assigned by a @JAttribute");
			
			final String selector = jselector.value().isBlank()
					? field.getName()
					: jselector.value();
			
			// Recursively call on child object
			field.set(object, getObjectFromElement(root.selectFirst(selector), fieldType));
		}
	}
	
	/**
	 * Set a {@link Field}'s value using the proper date conversion.
	 * Only works on Java 8 date types.
	 * 
	 * @param <T> The type of the object who's value is being set
	 * @param field The field
	 * @param object The object being set
	 * @param rawValue The date value in its raw form (string)
	 * @param format The date format specifier
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private <T> void setFieldDate(Field field, T object, String rawValue, JDateFormat format) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		final Class<?> dateType = field.getType();
		
		try {
			final Method parser = format == null 
					? dateType.getDeclaredMethod("parse", CharSequence.class)
					: dateType.getDeclaredMethod("parse", CharSequence.class, DateTimeFormatter.class);
			
			field.set(object, format == null 
							// <java 8 date object>.parse() is always static
							? parser.invoke(null, rawValue) 
							: parser.invoke(null, rawValue, DateTimeFormatter.ofPattern(format.value())));
			
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}
}
