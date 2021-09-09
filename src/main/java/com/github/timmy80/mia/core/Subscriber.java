package com.github.timmy80.mia.core;

/**
 * Event handler for publish messages
 * @author anthony
 *
 */
public interface Subscriber<T> {
	
	public void eventReceivePublish(MessageCtx<?> context, T request);
	
}
