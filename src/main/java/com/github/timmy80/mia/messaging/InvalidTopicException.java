package com.github.timmy80.mia.messaging;

/**
 * Exception class for invalid topic
 * @author anthony
 *
 */
public class InvalidTopicException extends IllegalArgumentException {

	private static final long serialVersionUID = -8183024833920827961L;
	
	/**
	 * Constructor
	 * @param message exception message
	 */
	public InvalidTopicException(String message) {
		super(message);
	}
}
