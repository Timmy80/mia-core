package com.github.timmy80.mia.messaging;

public class InvalidTopicException extends IllegalArgumentException {

	private static final long serialVersionUID = -8183024833920827961L;

	public InvalidTopicException(String message) {
		super(message);
	}
}
