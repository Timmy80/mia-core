package com.github.timmy80.mia.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.github.timmy80.mia.core.Async.Function1;
import com.github.timmy80.mia.core.Async.Function2;
import com.github.timmy80.mia.core.Async.Function3;
import com.github.timmy80.mia.core.Async.Function4;
import com.github.timmy80.mia.core.Async.ThrowingRunnable;
import com.github.timmy80.mia.core.Async.VoidFunction1;
import com.github.timmy80.mia.core.Async.VoidFunction2;
import com.github.timmy80.mia.core.Async.VoidFunction3;
import com.github.timmy80.mia.core.Async.VoidFunction4;

import io.netty.util.Timeout;

/**
 * Base class of a {@link Terminal} {@link State}
 * @author anthony
 *
 */
public abstract class TerminalState extends State {
	
	private Terminal<?> terminal = null;
	
	/**
	 * Default constructor
	 */
	public TerminalState() {
		
	}
	
	/**
	 * protected method for the parent Terminal of this state.
	 * @param t The parent {@link Terminal} of this State
	 */
	protected void setTerminal(Terminal<?> t) {
		this.terminal = t;
	}
	
	/**
	 * Get the terminal of this State
	 * @return a Terminal
	 */
	public Terminal<?> terminal(){
		return this.terminal;
	}
	
	/**
	 * Get the task of this {@link Terminal}
	 * @return a Task
	 */
	public Task task(){
		return this.terminal.task();
	}
	
	/**
	 * Is this terminal active.
	 * @return True is the Terminal is active
	 */
	public boolean isActive() {
		return this.terminal.getState() == this;
	}
	
	//#region
	
	/**
	 * Implementation of {@linkplain Async#callBefore(Executor, TimeLimit, Callable)} with a checks to ensure the State is active and the
	 * terminal not terminated.<br>
	 * <br>
	 * If at the execution time the state is inactive then the call will be completed exceptionally by an InactiveStateException.<br>
	 * If at the execution time the teminal is terminated then the call will be completed exceptionally by an TerminatedTerminalException.
	 * @param <R> The return Type
	 * @param limit The TimeLimit for this call.
	 * @param callable The Callable to execute.
	 * @return a Completable future.
	 */
	public <R> CompletableFuture<R> callBefore(TimeLimit limit, Callable<R> callable){
		
		return Async.callBefore(task(), limit, () -> {
			if(!isActive())
				throw new InactiveStateException(this);
			if(terminal.isTerminated())
				throw new TerminatedTerminalException(terminal);
			return callable.call();
		});
	}
	
	//***************************************************************************
	// The following methods are the copied from Async and made non static to use the terminal as an executor.
	//***************************************************************************
	
	/**
	 * pass the given runnable to this {@link ExecutionStage}
	 * @param runnable the {@link ThrowingRunnable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public CompletableFuture<Void> runLater(ThrowingRunnable runnable){
		return runBefore(TimeLimit.noLimit(), runnable);
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <T0> Type of the first function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0> CompletableFuture<Void> runLater(VoidFunction1<T0> function, T0 arg0){
		return runLater(() -> function.apply(arg0));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0, T1> CompletableFuture<Void> runLater(VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runLater(() -> function.apply(arg0, arg1));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0, T1, T2> CompletableFuture<Void> runLater(VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runLater(() -> function.apply(arg0, arg1, arg2));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @param arg3 the forth function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0, T1, T2, T3> CompletableFuture<Void> runLater(VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runLater(() -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	/**
	 * pass the given {@link Callable} to this {@link ExecutionStage}
	 * @param <R> Return type of the function
	 * @param callable the {@link Callable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <R> CompletableFuture<R> callLater(Callable<R> callable){
		return callBefore(TimeLimit.noLimit(), callable);
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <R, T0> CompletableFuture<R> callLater(Function1<R, T0> function, T0 arg0){
		return callLater(() -> function.apply(arg0));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <R, T0, T1> CompletableFuture<R> callLater(Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return callLater(() -> function.apply(arg0, arg1));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <R, T0, T1, T2> CompletableFuture<R> callLater(Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return callLater(() -> function.apply(arg0, arg1, arg2));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @param arg3 the forth function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <R, T0, T1, T2, T3> CompletableFuture<R> callLater(Function4<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return callLater(() -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	/**
	 * pass the given {@link Runnable} to this {@link ExecutionStage} with a {@link TimeLimit}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param runnable the {@link Runnable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public CompletableFuture<Void> executeBefore(TimeLimit limit, Runnable runnable){
		return runBefore(limit, ()-> runnable.run());
	}
	
	/**
	 * pass the given runnable to this {@link ExecutionStage} with a {@link TimeLimit}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param runnable the {@link ThrowingRunnable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public CompletableFuture<Void> runBefore(TimeLimit limit, ThrowingRunnable runnable){
		return callBefore(limit, ()->{ runnable.run(); return null; });
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction1<T0> function, T0 arg0){
		return runBefore(limit, () -> function.apply(arg0));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0, T1> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runBefore(limit, () -> function.apply(arg0, arg1));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0, T1, T2> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runBefore(limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	/**
	 * pass the given function to this {@link ExecutionStage} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @param arg3 the forth function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public <T0, T1, T2, T3> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runBefore(limit, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on this {@link ExecutionStage}.<br>
	 * @param delayms the delay of this timer in milliseconds
	 * @param task the function called after the delay is reached
	 * @return a {@link Timeout} for the given args
	 */
	public Timeout newTimeout(long delayms, TimerTask task) {
		return newTimeout(delayms, TimeUnit.MILLISECONDS, task); 
	}
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on this {@link ExecutionStage}.<br>
	 * @param delay the delay of this timer
	 * @param unit the unit of the delay
	 * @param task the function called after the delay is reached
	 * @return a {@link Timeout} for the given args
	 */
	public Timeout newTimeout(long delay, TimeUnit unit, TimerTask task) {
		return Async.newTimeout(task(), delay, unit, t -> {
			runLater(task::run, t);
		});
	}

	//#endregion

}
