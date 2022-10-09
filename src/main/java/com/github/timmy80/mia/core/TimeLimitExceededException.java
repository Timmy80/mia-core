package com.github.timmy80.mia.core;

import io.netty.util.internal.UnstableApi;

/**
 * Exception thrown if the {@linkplain TimeLimit} given to a call to {@link Async#callBefore} or {@link Async#runBefore} is exceeded before or during execution.
 * @author anthony
 *
 */
public class TimeLimitExceededException extends RuntimeException {

	private static final long serialVersionUID = -5139094419508769646L;

	/**
	 * to keep ?
	 */
	private boolean executed;
	/**
	 * True if the execution has been interupted
	 */
	private final boolean interupted;
	
	/**
	 * To be removed ?
	 * @param executed unused
	 * @param interupted true if the execution has been interrupted
	 */
	@UnstableApi
	public TimeLimitExceededException(boolean executed, boolean interupted) {
		this.executed = executed;
		this.interupted = interupted;
	}

	/**
	 * To be removed ?
	 * @param executed unused
	 * @param interupted true if the execution has been interrupted
	 * @param cause The InterruptedException when interupted
	 */
	@UnstableApi
	public TimeLimitExceededException(boolean executed, boolean interupted, Throwable cause) {
		super(cause);
		this.executed = executed;
		this.interupted = interupted;
	}
	
	/**
	 * Constructor
	 * @param interupted true if the execution has been interrupted
	 */
	public TimeLimitExceededException(boolean interupted) {
		this.interupted = interupted;
	}
	
	/**
	 * Constructor
	 * @param interupted true if the execution has been interrupted
	 * @param cause The InterruptedException when interupted
	 */
	public TimeLimitExceededException(boolean interupted, Throwable cause) {
		super(cause);
		this.interupted = interupted;
	}


	//TODO keep or remove this ? does not mean anything with current usage
	/**
	 * Check if the execution occurred anyway
	 * @return True if the code has been executed
	 */
	@UnstableApi
	public boolean isExecuted() {
		return executed;
	}

	/**
	 * Indicates if the Executor Thread as been interrupted on a blocking call during execution.
	 * @return true is the Executor Thread as been interrupted on a blocking call during execution.
	 */
	public boolean isInterupted() {
		return interupted;
	}
	
	
}
