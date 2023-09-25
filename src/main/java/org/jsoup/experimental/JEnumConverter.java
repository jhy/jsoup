package org.jsoup.experimental;

public interface JEnumConverter<E extends Enum<E>> {
	// (currently) there's no need to go backwards and get the raw value.
//	public String getRaw(E enumObj);
	
	public E getEnum(String raw);
}