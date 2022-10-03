package com.github.timmy80.mia.core;

import java.util.concurrent.Callable;
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

/**
 * A non Thread Executor which depends on a {@link Task} to be executed.<br>
 * Creating terminals allows to dispatch the management of complex sub-tasks in a logical context.<br>
 * Terminal provides a state mechanism which allows to further dispatch the complexity.<br>
 * <br>
 * A Terminal has always a {@link State} unless terminated. On Terminal creation the state is {@link InitialState}.
 * @author anthony
 *
 * @param <T>
 */
public class Terminal<T extends Task> implements ExecutionStage {

	private static Logger logger = LogManager.getLogger(Terminal.class.getName());
	
	protected final T task; 
	private State state = new InitialState();
	
	public Terminal(T task) {
		this.task = task;
		task.registerTerminal(this);
	}
	
	/**
	 * Change the state of the Terminal.<br> 
	 * On call to this method the previous state is immediately considered inactive and the next state active even though the methods 
	 * {@link State#eventLeaveState()} and {@link State#eventEntry()} have not been called yet on the previous state and the next state respectively.<br>
	 * <br>
	 * @param nextState Providing a null State to this method is equivalent to a call on {@link Terminal#terminate()}
	 */
	public void nextState(TerminalState nextState) {
		State prevState = this.state;
		this.state = nextState;
		
		logger.debug("{} {}", this, new LogFmt()
				.append("prevState", prevState.getName())
				.append("event", "terminal entering new state"));
		
		if(prevState != null) {
			prevState.eventLeaveState();
		}
		
		if(this.state == null) {
			// end of terminal
			logger.debug("{} {}", this, new LogFmt().append("event", "End of terminal"));
			task.unRegisterTerminal(this);
			this.eventTermination();
			
			return; // avoid further execution
		}
		
		nextState.setTerminal(this);
		
		try {
			this.state.eventEntry();
		} catch (Throwable e) {
			logger.error("{} {}", this, new LogFmt()
					.append("event", "Unexpected error thrown by eventEntry")
					.append("error", e.getMessage())
					.append("exception", e.getClass().getName()));
			logger.throwing(Level.ERROR, e);
		}
	}
	
	/**
	 * End the life of this terminal.<br>
	 * A call on {@link State#eventLeaveState()} will be done on the active {@link State} and then {@link Terminal#eventTermination()} will be called.
	 */
	public void terminate() {
		nextState(null);
	}
	
	public boolean isTerminated() {
		return this.state == null;
	}

	@Override
	public boolean isActive() {
		return !isTerminated();
	}
	
	/**
	 * Event method called when the stop sequence of the parent {@link Task} is triggered.<br>
	 * You can safely override this method to handle the event.<br>
	 * In order to prevent an immediate stop, you may add one or more epilogs to this Task by calling {@link #registerEpilog(CompletableFuture)}
	 */
	protected void eventStopRequested() {
	}
	
	/**
	 * Event method called when the Terminal exited its last {@link State}.<br>
	 * You can safely override this method to release resources owned by the terminal.
	 */
	protected void eventTermination() {
	};
	
	/**
	 * Override this method to provide an identifier for this terminal.<br>
	 * The id is null by default.
	 * @return
	 */
	public String getId() {
		return null;
	}
	
	public State getState() {
		return this.state;
	}
	
	public T task() {
		return task;
	}
	
	@Override
	public String toString() {
		String strState = (isTerminated())? "TERMINATED": state.getName();
		LogFmt fmt = new LogFmt()
				.append("type", getClass().getSimpleName())
				.append("state", strState)
				.append("id", getId());
		
		return fmt.toString();
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
	// Short calls to Async static methods on this ExecutionStage.
	//***************************************************************************
	
	/**
	 * Implementation of {@linkplain Async#callBefore(Executor, TimeLimit, Callable)} with a check to ensure the Terminal is not terminated.<br>
	 * <br>
	 * If at the execution time the teminal is terminated then the call will be completed exceptionally by an TerminatedTerminalException.
	 * @param <R> The return Type
	 * @param limit The TimeLimit for this call.
	 * @param callable The Callable to execute.
	 * @return a Completable future.
	 */
	public <R> CompletableFuture<R> callBefore(TimeLimit limit, Callable<R> callable){
		
		return Async.callBefore(task(), limit, () -> {
			if(isTerminated())
				throw new TerminatedTerminalException(this);
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
		return Async.newTimeout(task(), delay, unit, t -> {
			runLater(task::run, t);
		});
	}
	
	
	//#endregion
}
