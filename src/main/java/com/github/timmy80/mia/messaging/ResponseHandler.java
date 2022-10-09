package com.github.timmy80.mia.messaging;

/**
 * Message response handler interface
 * @author anthony
 *
 * @param <T> The type of the response obejct
 */
@FunctionalInterface
public interface ResponseHandler<T> {
	/**
	 * Event callback method called when a response is received
	 * @param context the message context
	 * @param response the response object
	 */
	public void eventResponseReceived(MessageCtx<T> context, T response);
}
