package com.github.timmy80.mia.core;

/**
 * A very simple base class to create States for {@link Terminal}.
 * @author anthony
 *
 */
public abstract class State implements ExecutionStage {
	
	/**
	 * Method called by the Terminal when entering this Sate.
	 */
	protected abstract void eventEntry();
	
	/**
	 * Override this method to implement some actions when leaving this State
	 */
	protected void eventLeaveState() {
		// nothing to be done by default
	}
	
	public String getName() {
		return this.getClass().getSimpleName();
	}
	
	@Override
	public String toString() {
		return getClass().getName();
	}
}
