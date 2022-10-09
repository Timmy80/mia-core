package com.github.timmy80.mia.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
	 */
	@FunctionalInterface
	public static interface ThrowingRunnable{
		/**
		 * Run the code.
		 * @throws Exception any exception thrown during the execution
		 */
		public void run() throws Exception;
	}
	
	/**
	 * A void returning {@link Function} that supports {@link Exception}
	 * @author anthony
	 * @param <T0> Type of the first function arg
	 */
	@FunctionalInterface
	public static interface VoidFunction1<T0> {
		/**
		 * Applies this function to the given argument.
		 * @param arg0 the first function arg
		 * @throws Exception any exception thrown during the execution
		 */
		public void apply(T0 arg0) throws Exception;
	}

	/**
	 * A void returning {@link Function} that supports {@link Exception} and 2 args
	 * @author anthony
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 */
	@FunctionalInterface
	public static interface VoidFunction2<T0,  T1> {
		/**
		 * Applies this function to the given arguments.
		 * @param arg0 the first function arg
		 * @param arg1 the second function arg
		 * @throws Exception any exception thrown during the execution
		 */
		public void apply(T0 arg0, T1 arg1) throws Exception;
	}
	
	/**
	 * A void returning {@link Function} that supports {@link Exception} and 3 args
	 * @author anthony
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 */
	@FunctionalInterface
	public static interface VoidFunction3<T0, T1, T2> {
		/**
		 * Applies this function to the given arguments.
		 * @param arg0 the first function arg
		 * @param arg1 the second function arg
		 * @param arg2 the third function arg
		 * @throws Exception any exception thrown during the execution
		 */
		public void apply(T0 arg0, T1 arg1, T2 arg2) throws Exception;
	}
	
	/**
	 * A void returning {@link Function} that supports {@link Exception} and 4 args
	 * @author anthony
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 */
	@FunctionalInterface
	public static interface VoidFunction4<T0, T1, T2, T3> {
		/**
		 * Applies this function to the given arguments.
		 * @param arg0 the first function arg
		 * @param arg1 the second function arg
		 * @param arg2 the third function arg
		 * @param arg3 the forth function arg
		 * @throws Exception any exception thrown during the execution
		 */
		public void apply(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Exception;
	}
	
	/**
	 * A {@link Function} that supports {@link Exception}
	 * @author anthony
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 */
	@FunctionalInterface
	public static interface Function1<R, T0> {		
		/**
		 * Applies this function to the given argument.
		 * @param arg0 the first function arg
		 * @throws Exception any exception thrown during the execution
		 * @return the function result
		 */
		public R apply(T0 arg0) throws Exception;
	}

	/**
	 * A {@link Function} that supports {@link Exception} and 2 args
	 * @author anthony
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 */
	@FunctionalInterface
	public static interface Function2<R, T0,  T1> {
		/**
		 * Applies this function to the given arguments.
		 * @param arg0 the first function arg
		 * @param arg1 the second function arg
		 * @throws Exception any exception thrown during the execution
		 * @return the function result
		 */
		public R apply(T0 arg0, T1 arg1) throws Exception;
	}
	
	/**
	 * A {@link Function} that supports {@link Exception} and 3 args
	 * @author anthony
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 */
	@FunctionalInterface
	public static interface Function3<R, T0, T1, T2> {
		/**
		 * Applies this function to the given arguments.
		 * @param arg0 the first function arg
		 * @param arg1 the second function arg
		 * @param arg2 the third function arg
		 * @throws Exception any exception thrown during the execution
		 * @return the function result
		 */
		public R apply(T0 arg0, T1 arg1, T2 arg2) throws Exception;
	}
	
	/**
	 * A {@link java.util.function.Function} that supports {@link Exception} and 4 args
	 * @author anthony
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 */
	@FunctionalInterface
	public static interface Function4<R, T0, T1, T2, T3> {
		/**
		 * Applies this function to the given arguments.
		 * @param arg0 the first function arg
		 * @param arg1 the second function arg
		 * @param arg2 the third function arg
		 * @param arg3 the forth function arg
		 * @throws Exception any exception thrown during the execution
		 * @return the function result
		 */
		public R apply(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Exception;
	}
	
	//#endregion
	
	//#region
	//***************************************************************************
	// Static Methods for using Functional Interfaces. 
	// ex: Async.runLater(Executors.newSingleThreadExecutor(), obj::method, arg1, arg2)
	//***************************************************************************
	
	/**
	 * pass the given {@link Runnable} to an {@link Executor}
	 * @param executor the {@link Executor}
	 * @param runnable the {@link Runnable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static CompletableFuture<Void> execute(Executor executor, Runnable runnable){
		return runLater(executor, ()-> runnable.run());
	}
	
	/**
	 * pass the given runnable to to an {@link Executor}
	 * @param executor the {@link Executor}
	 * @param runnable the {@link ThrowingRunnable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static CompletableFuture<Void> runLater(Executor executor, ThrowingRunnable runnable){
		return runBefore(executor, TimeLimit.noLimit(), runnable);
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <T0> Type of the first function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0> CompletableFuture<Void> runLater(Executor executor, VoidFunction1<T0> function, T0 arg0){
		return runLater(executor, () -> function.apply(arg0));
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0, T1> CompletableFuture<Void> runLater(Executor executor, VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runLater(executor, () -> function.apply(arg0, arg1));
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0, T1, T2> CompletableFuture<Void> runLater(Executor executor, VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runLater(executor, () -> function.apply(arg0, arg1, arg2));
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @param arg3 the forth function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0, T1, T2, T3> CompletableFuture<Void> runLater(Executor executor, VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runLater(executor, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	/**
	 * pass the given {@link Callable} to an {@link Executor}
	 * @param <R> Return type of the function
	 * @param executor the {@link Executor}
	 * @param callable the {@link Callable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R> CompletableFuture<R> callLater(Executor executor, Callable<R> callable){
		return callBefore(executor, TimeLimit.noLimit(), callable);
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R, T0> CompletableFuture<R> callLater(Executor executor, Function1<R, T0> function, T0 arg0){
		return callLater(executor, () -> function.apply(arg0));
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R, T0, T1> CompletableFuture<R> callLater(Executor executor, Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return callLater(executor, () -> function.apply(arg0, arg1));
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R, T0, T1, T2> CompletableFuture<R> callLater(Executor executor, Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return callLater(executor, () -> function.apply(arg0, arg1, arg2));
	}
	
	/**
	 * pass the given function to to an {@link Executor}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 * @param executor the {@link Executor}
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @param arg3 the forth function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R, T0, T1, T2, T3> CompletableFuture<R> callLater(Executor executor, Function4<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return callLater(executor, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	/**
	 * pass the given {@link Runnable} to an {@link Executor} with a {@link TimeLimit}
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param runnable the {@link Runnable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static CompletableFuture<Void> executeBefore(Executor executor, TimeLimit limit, Runnable runnable){
		return runBefore(executor, limit, ()-> runnable.run());
	}
	
	/**
	 * pass the given runnable to to an {@link Executor} with a {@link TimeLimit}
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param runnable the {@link ThrowingRunnable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, ThrowingRunnable runnable){
		return callBefore(executor, limit, ()->{ runnable.run(); return null; });
	}
	
	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction1<T0> function, T0 arg0){
		return runBefore(executor, limit, () -> function.apply(arg0));
	}

	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0, T1> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction2<T0, T1> function, T0 arg0, T1 arg1){
		return runBefore(executor, limit, () -> function.apply(arg0, arg1));
	}

	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0, T1, T2> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction3<T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return runBefore(executor, limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @param arg3 the forth function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <T0, T1, T2, T3> CompletableFuture<Void> runBefore(Executor executor, TimeLimit limit, VoidFunction4<T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3){
		return runBefore(executor, limit, () -> function.apply(arg0, arg1, arg2, arg3));
	}
	
	/**
	 * pass the given {@link Callable} to an {@link Executor} with a {@link TimeLimit}
	 * @param <R> Return type of the function
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param callable the {@link Callable}
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Callable<R> callable){
		CompletableFuture<R> future = new CompletableFuture<R>();
		executor.execute(() -> {
			Timeout t = null;
			try {
				if(limit.isExpired())
					throw new TimeLimitExceededException(false);
				
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
				future.completeExceptionally(new TimeLimitExceededException(true, e));
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
	
	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R, T0> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Function1<R, T0> function, T0 arg0){
		return callBefore(executor, limit, () -> function.apply(arg0));
	}
	
	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R, T0, T1> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Function2<R, T0, T1> function, T0 arg0, T1 arg1){
		return callBefore(executor, limit, () -> function.apply(arg0, arg1));
	}
	
	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
	public static <R, T0, T1, T2> CompletableFuture<R> callBefore(Executor executor, TimeLimit limit, Function3<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2){
		return callBefore(executor, limit, () -> function.apply(arg0, arg1, arg2));
	}
	
	/**
	 * pass the given function to to an {@link Executor} with a {@link TimeLimit}
	 * @param <R> Return type of the function
	 * @param <T0> Type of the first function arg
	 * @param <T1> Type of the second function arg
	 * @param <T2> Type of the third function arg
	 * @param <T3> Type of the forth function arg
	 * @param executor the {@link Executor}
	 * @param limit the {@link TimeLimit} for the execution
	 * @param function the function
	 * @param arg0 the first function arg
	 * @param arg1 the second function arg
	 * @param arg2 the third function arg
	 * @param arg3 the forth function arg
	 * @return A {@link CompletableFuture} for the execution
	 */
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
	 * @return a {@link Timeout} for the given args
	 */
	public static Timeout newTimeout(long delay, TimeUnit unit, TimerTask timerTask) {
		return timer.newTimeout(timerTask, delay, unit);
	}
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on the thread of the timer.<br>
	 * If you need your timeout to be handled on a specific thread use {@linkplain Async#newTimeout(Executor, long, TimerTask)}
	 * @param delayms the delay of this timer in milliseconds
	 * @param timerTask the function called after the delay is reached
	 * @return a {@link Timeout} for the given args
	 */
	public static Timeout newTimeout(long delayms, TimerTask timerTask) {
		return newTimeout(delayms, TimeUnit.MILLISECONDS, timerTask);
	}
	
	/**
	 * Create a new Timeout. If the delay is reached, the timerTask is called on the given executor. 
	 * @param executor the executor that will call the timeoutFunction
	 * @param delayms the delay of this timer in milliseconds
	 * @param timerTask the function called after the delay is reached
	 * @return a {@link Timeout} for the given args
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
	 * @return a {@link Timeout} for the given args
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
