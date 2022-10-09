package com.github.timmy80.mia.core;

/**
 * Exception class thrown when an execution is attempted on an inactive {@link Terminal}
 * @author anthony
 *
 */
public class TerminatedTerminalException extends RuntimeException {

	private static final long serialVersionUID = -5567420281871226466L;

	/**
	 * Constructor
	 * @param terminal the inactive terminal on which an execution was attempted
	 */
	public TerminatedTerminalException(Terminal<?> terminal) {
		super(String.format("Call avoided on terminated terminal: %s", terminal.toString()));
	}
}
