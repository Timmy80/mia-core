package com.github.timmy80.mia.core;

/**
 * A simple event synchronization block.
 * @author anthony
 *
 */
public class EventControlBlock {
	boolean eventPosted;
	
	public synchronized void post() {
		eventPosted = true;
		this.notify();
	}
	
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
