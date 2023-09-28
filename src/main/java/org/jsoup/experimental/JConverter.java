package org.jsoup.experimental;

/**
 * Provides a common way to deserialize an XML
 * value from its "raw" form to the specified Java object
 * @author Sam
 *
 * @param <T>
 */
public interface JConverter<T> {
	// (currently) there's no need to go backwards and get the raw value.
//	public String getRaw(E enumObj);
	
	public T getConvertedValue(String raw);
}