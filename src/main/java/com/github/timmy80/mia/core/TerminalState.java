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

public abstract class TerminalState extends State {
	
	private Terminal<?> terminal = null;
	
	protected void setTerminal(Terminal<?> t) {
		this.terminal = t;
	}
	
	public Terminal<?> terminal(){
		return this.terminal;
	}
	
	public Task task(){
		return this.terminal.task();
	}
	
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
	
	public CompletableFuture<Void> runLater(ThrowingRunnable runnable){
		return runBefore(TimeLimit.noLimit(), runnable);
	}
	
	public <T0> CompletableFuture<Void> runLater(VoidFunction1<T0> function, T0 arg0){
		return runLater(() -> function.apply(arg0));
	}
	
	public <T0, T1> CompletableFuture<Void> runLater(VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runLater(() -> function.apply(arg0, arg1));
	}
	
	public <T0, T1, T2> CompletableFuture<Void> runLater(VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runLater(() -> function.apply(arg0, arg1, arg2));
	}
	
	public <T0, T1, T2, T3> CompletableFuture<Void> runLater(VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runLater(() -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public <R> CompletableFuture<R> callLater(Callable<R> callable){
		return callBefore(TimeLimit.noLimit(), callable);
	}
	
	public <R, T0> CompletableFuture<R> callLater(Function1<R, T0> function, T0 arg0){
		return callLater(() -> function.apply(arg0));
	}
	
	public <R, T0, T1> CompletableFuture<R> callLater(Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return callLater(() -> function.apply(arg0, arg1));
	}
	
	public <R, T0, T1, T2> CompletableFuture<R> callLater(Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return callLater(() -> function.apply(arg0, arg1, arg2));
	}
	
	public <R, T0, T1, T2, T3> CompletableFuture<R> callLater(Function4<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return callLater(() -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public CompletableFuture<Void> executeBefore(TimeLimit limit, Runnable runnable){
		return runBefore(limit, ()-> runnable.run());
	}
	
	public CompletableFuture<Void> runBefore(TimeLimit limit, ThrowingRunnable runnable){
		return callBefore(limit, ()->{ runnable.run(); return null; });
	}
	
	public <T0> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction1<T0> function, T0 arg0){
		return runBefore(limit, () -> function.apply(arg0));
	}
	
	public <T0, T1> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runBefore(limit, () -> function.apply(arg0, arg1));
	}
	
	public <T0, T1, T2> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runBefore(limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	public <T0, T1, T2, T3> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runBefore(limit, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public Timeout newTimeout(long delayms, TimerTask task) {
		return newTimeout(delayms, TimeUnit.MILLISECONDS, task); 
	}
	
	public Timeout newTimeout(long delay, TimeUnit unit, TimerTask task) {
		return newTimeout(delay, unit, t -> {
			runLater(task::run, t);
		});
	}

	//#endregion

}
