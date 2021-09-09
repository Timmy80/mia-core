package com.github.timmy80.mia.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.timmy80.mia.core.Async.Function1;
import com.github.timmy80.mia.core.Async.Function2;
import com.github.timmy80.mia.core.Async.Function3;
import com.github.timmy80.mia.core.Async.Function4;
import com.github.timmy80.mia.core.Async.ThrowingRunnable;
import com.github.timmy80.mia.core.Async.VoidFunction1;
import com.github.timmy80.mia.core.Async.VoidFunction2;
import com.github.timmy80.mia.core.Async.VoidFunction3;
import com.github.timmy80.mia.core.Async.VoidFunction4;

import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.concurrent.Future;


public class Terminal<T extends Task> implements Executor {

	private static Logger logger = LogManager.getLogger(Terminal.class.getName());
	
	protected final T task; 
	private State state = null;
	
	public Terminal(T task) {
		this.task = task;
	}
	
	public void nextState(State nextState) {
		if(state != null)
			state.eventLeaveState();
		
		this.state = nextState;
		
		if(this.state == null) {
			// end of terminal
			logger.debug("End of terminal: {}", this);
			eventEndOfTerminal();
			return;
		}
		
		try {
			logger.debug("{} entering state {}", this, this.state);
			this.state.eventEntry();
		} catch (Throwable e) {
			logger.error("Unexpected error at state {} eventEntry {}", this.state, e);
			logger.throwing(Level.ERROR, e);
		}
	}
	
	public void eventEndOfTerminal() {};
	
	public State getState() {
		return this.state;
	}
	
	public T task() {
		return task;
	}
	
	@Override
	public String toString() {
		return getClass().getName();
	}
	
	//#region
	//***************************************************************************
	// Future management
	//***************************************************************************
	
	public  <T0> void listenFuture(Future<T0> future, VoidFunction1<Future<T0>> method) {
		task.listenFuture(future, method);
	}
	
	public void listenFuture(ChannelFuture future, VoidFunction1<ChannelFuture> method) {
		task.listenFuture(future, method);
	}
	
	public  <T0> void listenFuture(CompletableFuture<T0> future, VoidFunction1<CompletableFuture<T0>> method) {
		task.listenFuture(future, method);
	}

	//#endregion
	
	//#region
	//***************************************************************************
	// Short calls to Async static methods on this Executor.
	//***************************************************************************
	
	/**
	 * Execute the given command at some time in the future using the Thread of this Task.<br>
	 * Implementation of {@link Executor}
	 */
	public void execute(Runnable e) {
		task().execute(e);
	}
	
	//***************************************************************************
	// The following methods are copied from Task
	//***************************************************************************
	
	public CompletableFuture<Void> runLater(ThrowingRunnable runnable){
		return Async.runLater(this, runnable);
	}
	
	public <T0> CompletableFuture<Void> runLater(VoidFunction1<T0> function, T0 arg0){
		return Async.runLater(this, () -> function.apply(arg0));
	}
	
	public <T0, T1> CompletableFuture<Void> runLater(VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return Async.runLater(this, () -> function.apply(arg0, arg1));
	}
	
	public <T0, T1, T2> CompletableFuture<Void> runLater(VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return Async.runLater(this, () -> function.apply(arg0, arg1, arg2));
	}
	
	public <T0, T1, T2, T3> CompletableFuture<Void> runLater(VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return Async.runLater(this, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public <R, T0> CompletableFuture<R> callLater(Function1<R, T0> function, T0 arg0){
		return Async.callLater(this, () -> function.apply(arg0));
	}
	
	public <R, T0, T1> CompletableFuture<R> callLater(Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return Async.callLater(this, () -> function.apply(arg0, arg1));
	}
	
	public <R, T0, T1, T2> CompletableFuture<R> callLater(Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return Async.callLater(this, () -> function.apply(arg0, arg1, arg2));
	}
	
	public <R, T0, T1, T2, T3> CompletableFuture<R> callLater(Function4<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return Async.callLater(this, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public CompletableFuture<Void> executeBefore(TimeLimit limit, Runnable runnable){
		return Async.runBefore(this, limit, ()-> runnable.run());
	}

	public <T0> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction1<T0> function, T0 arg0){
		return Async.runBefore(this, limit, () -> function.apply(arg0));
	}
	
	public <T0, T1> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return Async.runBefore(this, limit, () -> function.apply(arg0, arg1));
	}
	
	public <T0, T1, T2> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return Async.runBefore(this, limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	public <T0, T1, T2, T3> CompletableFuture<Void> runBefore(TimeLimit limit, VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return Async.runBefore(this, limit, () -> function.apply(arg0, arg1, arg2, arg3));
	}

	public <R, T0> CompletableFuture<R> callBefore(TimeLimit limit, Function1<R, T0> function, T0 arg0){
		return Async.callBefore(this, limit, () -> function.apply(arg0));
	}
	
	public <R, T0, T1> CompletableFuture<R> callBefore(TimeLimit limit, Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return Async.callBefore(this, limit, () -> function.apply(arg0, arg1));
	}
	
	public <R, T0, T1, T2> CompletableFuture<R> callBefore(TimeLimit limit, Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return Async.callBefore(this, limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	public <R, T0, T1, T2, T3> CompletableFuture<R> callBefore(TimeLimit limit, Function4<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return Async.callBefore(this, limit, () -> function.apply(arg0, arg1, arg2, arg3));
	}	
	
	public static Timeout newTimeout(long delayms, TimerTask task) {
		return Async.newTimeout(delayms, task);
	}
	
	public static Timeout newTimeout(long delay, TimeUnit unit, TimerTask task) {
		return Async.newTimeout(delay, unit, task);
	}
	
	
	//#endregion
}
