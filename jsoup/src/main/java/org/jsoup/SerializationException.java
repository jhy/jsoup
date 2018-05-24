package org.jsoup;

/**
 * A SerializationException is raised whenever serialization of a DOM element fails. This exception usually wraps an
 * {@link java.io.IOException} that may be thrown due to an inaccessible output stream.
 */
public final class SerializationException extends RuntimeException {
	/**
	 * Creates and initializes a new serialization exception with no error message and cause.
	 */
	public SerializationException() {
		super();
	}

	/**
	 * Creates and initializes a new serialization exception with the given error message and no cause.
	 * 
	 * @param message
	 *            the error message of the new serialization exception (may be <code>null</code>).
	 */
	public SerializationException(String message) {
		super(message);
	}

	/**
	 * Creates and initializes a new serialization exception with the specified cause and an error message of
     * <code>(cause==null ? null : cause.toString())</code> (which typically contains the class and error message of
     * <code>cause</code>).
	 * 
	 * @param cause
	 *            the cause of the new serialization exception (may be <code>null</code>).
	 */
	public SerializationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates and initializes a new serialization exception with the given error message and cause.
	 * 
	 * @param message
	 *            the error message of the new serialization exception.
	 * @param cause
	 *            the cause of the new serialization exception.
	 */
	public SerializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
