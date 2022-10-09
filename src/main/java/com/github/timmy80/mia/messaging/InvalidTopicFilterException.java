package com.github.timmy80.mia.messaging;

/**
 * Exception class for invalid topic filter
 * @author anthony
 *
 */
public class InvalidTopicFilterException extends Exception {

	private static final long serialVersionUID = -5226109160713913033L;
	
	/**
	 * Constructor
	 * @param message exception message
	 */
	public InvalidTopicFilterException(String message) {
		super(message);
	}

}
