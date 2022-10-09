package com.github.timmy80.mia.core;

/**
 * A simple event synchronization block.
 * @author anthony
 *
 */
public class EventControlBlock {
	boolean eventPosted;
	
	/**
	 * Default constructor
	 */
	public EventControlBlock() {
		
	}
	
	/**
	 * Post an event of this control block.
	 */
	public synchronized void post() {
		eventPosted = true;
		this.notify();
	}
	
	/**
	 * Synchronize on this until an event is posted
	 * @param timeoutMillis wait timeout in milliseconds
	 * @throws InterruptedException see {@link Object#wait(long)}
	 */
	public synchronized void waitForPost(long timeoutMillis) throws InterruptedException {
		if(eventPosted) {
			eventPosted = false;
			return;
		}
		else {
			this.wait(timeoutMillis);
			eventPosted = false;
		}
	}
}
