package com.github.timmy80.mia.messaging;

@FunctionalInterface
public interface ResponseHandler<T> {
	public void eventResponseReceived(MessageCtx<T> context, T response);
}
