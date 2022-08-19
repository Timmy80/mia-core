package com.github.timmy80.mia.core;

public class TerminatedTerminalException extends RuntimeException {

	private static final long serialVersionUID = -5567420281871226466L;

	public TerminatedTerminalException(Terminal<?> terminal) {
		super(String.format("Call avoided on terminated terminal: %s", terminal.toString()));
	}
}
