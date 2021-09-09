package com.github.timmy80.mia.core;

/**
 * Exception thrown if the {@linkplain TimeLimit} given to a call to {@link Async#callBefore} or {@link Async#runBefore} is exceeded before or during execution.
 * @author anthony
 *
 */
public class TimeLimitExceededException extends RuntimeException {

	private static final long serialVersionUID = -5139094419508769646L;

	private final boolean executed;
	private final boolean interupted;
	
	public TimeLimitExceededException(boolean executed, boolean interupted) {
		this.executed = executed;
		this.interupted = interupted;
	}

	//TODO keep or remove this ? does not mean anything with current usage
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
