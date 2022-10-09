package com.github.timmy80.mia.messaging;

/**
 * Event handler for publish messages
 * @author anthony
 *
 * @param <T> The type of the request message
 */
public interface Subscriber<T> {
	
	/**
	 * Event callback method called when a publish is received
	 * @param context the message context
	 * @param request the request object
	 */
	public void eventReceivePublish(MessageCtx<?> context, T request);
	
}
