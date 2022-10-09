package com.github.timmy80.mia.messaging;

/**
 * Exception class for invalid publish attempts
 * @author anthony
 *
 */
public class InvalidPublishException extends Exception {

	private static final long serialVersionUID = 5036514804122738979L;

	/**
	 * Constructor
	 * @param cause the {@link Exception} cause
	 */
	public InvalidPublishException(Throwable cause) {
		super(cause);
	}
}
