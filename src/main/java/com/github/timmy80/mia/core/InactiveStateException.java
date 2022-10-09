package com.github.timmy80.mia.core;

/**
 * Exception thrown when a job is not executed because the state that requested the call is inactive.
 * @author anthony
 *
 */
public class InactiveStateException extends RuntimeException {

	private static final long serialVersionUID = 6504570750386613236L;
	
	/**
	 * Constructor
	 * @param state the inactive state on which an execution was attempted
	 */
	public InactiveStateException(State state) {
		super(String.format("Call avoided on inactive state: %s", state.getName()));
	}
}
