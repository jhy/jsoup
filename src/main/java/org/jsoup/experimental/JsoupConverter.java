package org.jsoup.experimental;

import static java.util.Map.entry;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Converts XML into Java objects.
 * @author Sam Hieken
 *
 */
public class JsoupConverter {
	private static final Map<Type, Type> PRIMITIVES = Map.ofEntries(
				entry(boolean.class, Boolean.class),
				entry(byte.class, Byte.class),
				entry(short.class, Short.class),
				entry(int.class, Integer.class),
				entry(long.class, Long.class),
				entry(double.class, Double.class),
				entry(float.class, Float.class),
				entry(char.class, Character.class)
			);
	
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
		
		for (Field field : getAllFields(type)) {
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
				final Element child = ja.selector().isEmpty() 
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
	
	/**
	 * Get every declared field in a class, including superclass fields.
	 * @return
	 */
	public static List<Field> getAllFields(Class<?> type) {
		final List<Field> allFields = new ArrayList<>();
		
		while (type != Object.class) {
			for (Field field : type.getDeclaredFields())
				allFields.add(field);
			
			type = type.getSuperclass();
		}
		
		return allFields;
	}
	
	private <T> void setField(Field field, T object, String rawValue, Element root) throws IllegalArgumentException, IllegalAccessException, ParseException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException {
		final Class<?> fieldType = field.getType();
		final JDateFormat dateFormat = field.getDeclaredAnnotation(JDateFormat.class);
		final JConvert converter = field.getDeclaredAnnotation(JConvert.class);
		final JEnumConvert enumConverter = field.getDeclaredAnnotation(JEnumConvert.class);
		final JSelector jselector = field.getDeclaredAnnotation(JSelector.class);
		
		field.setAccessible(true);
		
		// Priority 1: Get out immediately if our XML value is null.
		if (rawValue == null)
			return;
		
		// Priority 2: A user specified converter always takes priority
		// above default conversion, as well as Enum conversion.
		else if (converter != null) {
			final Class<? extends JConverter<?>> converterClass = converter.value();
			
			if (!typesMatch(getJConverterTypeArg(converterClass), field.getGenericType()))
				throw new IllegalArgumentException("The field's JConverter took a different argument than it's type (" + field.getGenericType() + ")");
				
			setField(field, object, rawValue, converterClass);
		}
		
		// Priority 3: Check + set if it's a String
		else if (fieldType == String.class)
			field.set(object, rawValue);
		
		// Priority 4: The field type isn't a String, so make sure it isn't empty.
		else if (rawValue.isEmpty())
			return;
		
		// Enum
		else if (Enum.class.isAssignableFrom(fieldType)) {
			// Ignore this Enum if no converter annotation was specified.
			// ...may want to remove and go w/ default (valueOf())?
			if (enumConverter == null)
				return;
			
			final Class<? extends JConverter<?>> converterClass = enumConverter.value().length == 0 
					? null : enumConverter.value()[0];
			
			if (!typesMatch(getJConverterTypeArg(converterClass), field.getGenericType()))
				throw new IllegalArgumentException("The field's JConverter took a different argument than it's type (" + field.getGenericType() + ")");
			
			setField(field, object, rawValue, converterClass);
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
			// XXX parseBoolean is stupid, and converts anything not equal to "true" to false
			field.set(object, Boolean.parseBoolean(rawValue));
		
		else if (fieldType == Character.class || fieldType == char.class)
			field.set(object, rawValue.charAt(0));
		
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
			
			final String selector = jselector.value().isEmpty()
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
			
			final String selector = jselector.value().isEmpty()
					? field.getName()
					: jselector.value();
			
			// Recursively call on child object
			field.set(object, getObjectFromElement(root.selectFirst(selector), fieldType));
		}
	}
	
	private boolean typesMatch(Type type1, Type type2) {
		if (type1.equals(type2)) return true;
		
		final Type type2Check = PRIMITIVES.get(type1);
		final Type type1Check = PRIMITIVES.get(type2);
		
		return type1.equals(type1Check) 
				|| type2.equals(type2Check);
	}

	private <T> void setField(Field field, T object, String rawValue, Class<? extends JConverter<?>> converter) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final Class<?> fieldType = field.getType();
		final Method converterMethod = converter == null
				? fieldType.getMethod("valueOf", String.class)
				: converter.getMethod("getConvertedValue", String.class);
		
		// If there's no converter, we use valueOf, which is static; thus the target
		// is null.
		final Object target = converter == null
				? null
				: converter.getConstructor().newInstance();
		
		try {
			field.set(object, converterMethod.invoke(target, rawValue));
		} catch (InvocationTargetException e) {
			// This becomes problematic when the converter returns a wrapper,
			// such as Integer, with a null value - invoke automatically tries to
			// convert to primitive (very hacky workaround).
			if (e.getCause() instanceof NullPointerException)
				return;
			
			throw e;
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
	
	/**
	 * Get every parent Type of this child (superclasses, interfaces, their superclasses & interfaces, etc.)
	 * @param child
	 * @return All parent types, or empty set if there are none (Object)
	 */
	private Set<Type> getAllParentTypes(Class<?> child) {
		// Base case
		if (child == Object.class)
			return Set.of();
		
		final Set<Type> types = new HashSet<>();
		final Class<?> superClass = child.getSuperclass();
		final Type[] interfaces = child.getGenericInterfaces();
		
		types.addAll(Arrays.asList(interfaces));
		if (superClass != null) {
			types.add(superClass);
			types.addAll(getAllParentTypes(superClass));
		}
		
		// No null check; interfaces returns empty array if no interfaces.
		for (Type type : interfaces)
			types.addAll(getAllParentTypes(getRawType(type)));
		
		return types;
	}
	
	private Type getJConverterTypeArg(Class<? extends JConverter<?>> impl) {
		// Check the interface tree, then check the superclass tree...
		final Set<Type> types = getAllParentTypes(impl);
		
		for (Type type : types)
			if (getRawType(type) == JConverter.class) {
				// No class param specified? The param is an object.
				if (!(type instanceof ParameterizedType))
					return Object.class;
				
				return ((ParameterizedType) type).getActualTypeArguments()[0];
			}
		
		return null;
	}
	
	private Class<?> getRawType(Type type) {
		if (type instanceof ParameterizedType)
			// ParameterizedTypeImpl's rawType field is a Class<?>
			// https://stackoverflow.com/questions/5767122/parameterizedtype-getrawtype-returns-j-l-r-type-not-class
			return (Class<?>) ((ParameterizedType) type).getRawType();
		
		else if (type instanceof Class<?>)
			return (Class<?>) type;
		
		throw new IllegalArgumentException("Couldn't handle type: " + type.getClass() + " (" + type + ")");
	}
}
