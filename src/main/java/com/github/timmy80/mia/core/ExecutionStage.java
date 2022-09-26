package com.github.timmy80.mia.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface ExecutionStage {
	
	/**
	 * Implementation of {@linkplain Async#callBefore(Executor, TimeLimit, Callable)} which uses a local {@link Executor} and can implement context related checks.
	 * <br>
	 * If at the execution time the teminal is terminated then the call will be completed exceptionally by an TerminatedTerminalException.
	 * @param <R> The return Type
	 * @param limit The {@link TimeLimit} for this call.
	 * @param callable The Callable to execute.
	 * @return a {@linkplain CompletableFuture}
	 */
	public <R> CompletableFuture<R> callBefore(TimeLimit limit, Callable<R> callable);
	
	/**
	 * Getter to know if the ExecutionStage is active.
	 * @return True if the ExecutionStage is active.
	 */
	public boolean isActive();
}
