package com.github.timmy80.mia.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;

/**
 * Utility class to manage calls to {@link FunctionalInterface} by an {@link Executor} with support for timeouts(See: {@linkplain TimeLimit}).<br>
 * This class offers FunctionalInterfaces for Void and NonVoid functions up to 4 arguments.<br>
 * <br>
 * You have access to a variety of methods named with the following principles:<br>
 * <ul>
 * <li>starts with <strong>run</strong> for void returning methods and <strong>call</strong> for non void methods.</li>
 * <li>ends with <strong>Before</strong> when the execution is time constrained and <strong>Later</strong> otherwise.</li>
 * </ul>
 * 
 * When the execution time is constrained in case of a timeout the CompletableFuture will be completed exceptionally by a 
 * {@link TimeLimitExceededException}<br>
 * <br>
 * Examples:
 * <pre>{@code
 * Async.callLater(executor, testObject::methodWithTwoArg, "foo", "bar").get();
 *Async.runBefore(executor, TimeLimit.in(100), testObject::eventUserAction, new Date(), "click").get();
 * }</pre>
 * 
 * @author anthony
 *
 */
public class Async {
	
	/**
	 * Timer for time constrained executions
	 */
	protected static final Timer timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
	
	//#region
	//***************************************************************************
	// Functional Interfaces
	//***************************************************************************
	
	/**
	 * A {@link Runnable} that supports {@link Exception}
	 * @author anthony
	 *
	 */
	@FunctionalInterface
	public static interface ThrowingRunnable{
		public void run() throws Exception;
	}
	
	@FunctionalInterface
	public static interface VoidFunction1<T0> {
		public void apply(T0 arg0) throws Exception;
	}

	@FunctionalInterface
	public static interface VoidFunction2<T0,  T1> {
		public void apply(T0 arg0, T1 arg1) throws Exception;
	}
	
	@FunctionalInterface
	public static interface VoidFunction3<T0, T1, T2> {
		public void apply(T0 arg0, T1 arg1, T2 arg2) throws Exception;
	}
	
	@FunctionalInterface
	public static interface VoidFunction4<T0, T1, T2, T3> {
		public void apply(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Exception;
	}
	
	@FunctionalInterface
	public static interface Function1<R, T0> {
		public R apply(T0 arg0) throws Exception;
	}

	@FunctionalInterface
	public static interface Function2<R, T0,  T1> {
		public R apply(T0 arg0, T1 arg1) throws Exception;
	}
	
	@FunctionalInterface
	public static interface Function3<R, T0, T1, T2> {
		public R apply(T0 arg0, T1 arg1, T2 arg2) throws Exception;
	}
	
	@FunctionalInterface
	public static interface Function4<R, T0, T1, T2, T3> {
		public R apply(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Exception;
	}
	
	//#endregion
	
	//#region
	//***************************************************************************
	// Static Methods for using Functional Interfaces. 
	// ex: Async.runLater(Executors.newSingleThreadExecutor(), obj::method, arg1, arg2)
	//***************************************************************************
	
	public static CompletableFuture<Void> execute(Executor executor, Runnable runnable){
		return runLater(executor, ()-> runnable.run());
	}
	
	public static CompletableFuture<Void> runLater(Executor executor, ThrowingRunnable runnable){
		return runBefore(executor, TimeLimit.noLimit(), runnable);
	}
	
	public static <T0> CompletableFuture<Void> runLater(Executor executor, VoidFunction1<T0> function, T0 arg0){
		return runLater(executor, () -> function.apply(arg0));
	}
	
	public static <T0, T1> CompletableFuture<Void> runLater(Executor executor, VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runLater(executor, () -> function.apply(arg0, arg1));
	}
	
	public static <T0, T1, T2> CompletableFuture<Void> runLater(Executor executor, VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runLater(executor, () -> function.apply(arg0, arg1, arg2));
	}
	
	public static <T0, T1, T2, T3> CompletableFuture<Void> runLater(Executor executor, VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runLater(executor, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public static <R> CompletableFuture<R> callLater(Executor executor, Callable<R> callable){
		return callBefore(executor, TimeLimit.noLimit(), callable);
	}
	
	public static <R, T0> CompletableFuture<R> callLater(Executor executor, Function1<R, T0> function, T0 arg0){
		return callLater(executor, () -> function.apply(arg0));
	}
	
	public static <R, T0, T1> CompletableFuture<R> callLater(Executor executor, Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return callLater(executor, () -> function.apply(arg0, arg1));
	}
	
	public static <R, T0, T1, T2> CompletableFuture<R> callLater(Executor executor, Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return callLater(executor, () -> function.apply(arg0, arg1, arg2));
	}
	
	public static <R, T0, T1, T2, T3> CompletableFuture<R> callLater(Executor executor, Function4<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return callLater(executor, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public static CompletableFuture<Void> executeBefore(Executor executor, TimeLimit limit, Runnable runnable){
		return runBefore(executor, limit, ()-> runnable.run());
	}
	
	public static CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, ThrowingRunnable runnable){
		return callBefore(executor, limit, ()->{ runnable.run(); return null; });
	}
	
	public static <T0> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction1<T0> function, T0 arg0){
		return runBefore(executor, limit, () -> function.apply(arg0));
	}
	
	public static <T0, T1> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runBefore(executor, limit, () -> function.apply(arg0, arg1));
	}
	
	public static <T0, T1, T2> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runBefore(executor, limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	public static <T0, T1, T2, T3> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runBefore(executor, limit, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	public static <R> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Callable<R> callable){
		CompletableFuture<R> future = new CompletableFuture<R>();
		executor.execute(() -> {
			Timeout t = null;
			try {
				if(limit.isExpired())
					throw new TimeLimitExceededException(false, false);
				
				final Thread thread = Thread.currentThread();
				if(!limit.isNoLimit()) {
					t = timer.newTimeout((Timeout timeout) ->{
						thread.interrupt(); // interrupt the thread in case of timeout to interrupt any blocking task
					}, limit.remaining(), TimeUnit.MILLISECONDS);
				}
				future.complete(callable.call());
				t.cancel(); // cancel timer ASAP to avoid false positive timeout.
			}
			catch(InterruptedException e) {
				future.completeExceptionally(new TimeLimitExceededException(false, true));
			}
			catch(Exception e) {
				future.completeExceptionally(e);
			}
			finally {
				if(t != null)
					t.cancel();
			}
		});
		return future;
	}
	
	public static <R, T0> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Function1<R, T0> function, T0 arg0){
		return callBefore(executor, limit, () -> function.apply(arg0));
	}
	
	public static <R, T0, T1> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return callBefore(executor, limit, () -> function.apply(arg0, arg1));
	}
	
	public static <R, T0, T1, T2> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return callBefore(executor, limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	public static <R, T0, T1, T2, T3> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Function4<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return callBefore(executor, limit, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	//#endregion
	
	//#region
	//***************************************************************************
	// Static Methods for Tiemout management. 
	// ex: Async.newTimeout(100, TimeUnit.MILLISECONDS, obj::onTimeoutEvent)
	//***************************************************************************
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on the thread of the timer.<br>
	 * If you need your timeout to be handled on a specific thread use {@linkplain Async#newTimeout(Executor, long, TimeUnit, TimerTask)}
	 * @param delay the delay of this timer
	 * @param unit the unit of the delay
	 * @param timerTask the function called after the delay is reached
	 * @return
	 */
	public static Timeout newTimeout(long delay, TimeUnit unit, TimerTask timerTask) {
		return timer.newTimeout(timerTask, delay, unit);
	}
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on the thread of the timer.<br>
	 * If you need your timeout to be handled on a specific thread use {@linkplain Async#newTimeout(Executor, long, TimerTask)}
	 * @param delay the delay of this timer in milliseconds
	 * @param timerTask the function called after the delay is reached
	 * @return
	 */
	public static Timeout newTimeout(long delayms, TimerTask timerTask) {
		return newTimeout(delayms, TimeUnit.MILLISECONDS, timerTask);
	}
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on the given executor. 
	 * @param executor the executor that will call the timeoutFunction
	 * @param delayms the delay of this timer in milliseconds
	 * @param timerTask the function called after the delay is reached
	 * @return
	 */
	public static Timeout newTimeout(Executor executor, long delayms, TimerTask timerTask) {
		return newTimeout(executor, delayms, TimeUnit.MILLISECONDS, timerTask); 
	}
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on the given executor. 
	 * @param executor the executor that will call the timeoutFunction
	 * @param delay the delay of this timer
	 * @param unit the unit of the delay
	 * @param timerTask the function called after the delay is reached
	 * @return
	 */
	public static Timeout newTimeout(Executor executor, long delay, TimeUnit unit, TimerTask timerTask) {
		return newTimeout(delay, unit, t -> {
			runLater(executor, timerTask::run, t);
		});
	}
	
	//#endregion
	
	/**
	 * Avoid any instance of this class to be made
	 */
	private Async() {
		// nothing to be done
	}
}
