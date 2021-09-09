package com.github.timmy80.mia.core;

public abstract class State {
	protected abstract void eventEntry();
	
	public String getName() {
		return this.getClass().getSimpleName();
	}
	
	/**
	 * Override this method to implement some actions when leaving this State
	 */
	protected void eventLeaveState() {
		// nothing to be done by default
	}
	
	@Override
	public String toString() {
		return getClass().getName();
	}
}
