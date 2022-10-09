package com.github.timmy80.mia.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.Timeout;
import io.netty.util.concurrent.Future;

/**
 * A MiA task. This is intended to be the single point of synchronization.
 * @author anthony
 *
 */
public abstract class Task extends Thread implements Executor, ExecutionStage {

	private static Logger logger = LogManager.getLogger(Task.class.getName());
	
	private ApplicationContext appCtx;

	/**
	 * Keep this to true in order to keep this thread alive.
	 */
	private boolean run = true;
	
	private boolean stopEventTriggered = false;
	
	ConcurrentLinkedQueue<Runnable> jobs = new ConcurrentLinkedQueue<>();
	ConcurrentLinkedQueue<CompletableFuture<?>> epilogs = new ConcurrentLinkedQueue<>();
	ArrayList<Terminal<?>> terminals = new ArrayList<>();

	EventControlBlock ecb = new EventControlBlock();
	
	/**
	 * Last time a full iteration has been performed (Timestamp in milliseconds)
	 */
	volatile long lastScanTime = 0;

	/**
	 * Constructor to associate this task to an {@link ApplicationContext}
	 * @param name The name of this Thread
	 * @param appCtx The application context of this Task
	 * @throws IllegalArgumentException If parameters are set incorrectly, preventing execution
	 */
	public Task(String name, ApplicationContext appCtx) throws IllegalArgumentException {
		super(name);
		this.appCtx = appCtx;
		this.appCtx.addTask(this);
	}
	
	/**
	 * Default constructor.<br>
	 * The ApplicationContext will be set to {@link ApplicationContext#getDefault()}.
	 * @param name Unique name of this Task.
	 * @throws IllegalArgumentException If parameters are set incorrectly, preventing execution
	 */
	public Task(String name) throws IllegalArgumentException {
		this(name, ApplicationContext.getDefault());
	}

	/**
	 * Wake up the thread of this task
	 */
	public void wakeup() {
		ecb.post();
	}
	
	/**
	 * Internal method for a terminal to register itself to its parent task
	 * @param terminal teminal to register
	 */
	protected void registerTerminal(Terminal<?> terminal) {
		terminals.add(terminal);
	}
	
	/**
	 * Internal method for a terminal to unregister itself to its parent task
	 * @param terminal terminal to unregister
	 */
	protected void unRegisterTerminal(Terminal<?> terminal) {
		terminals.remove(terminal);
	}
	
	/**
	 * Add an epilog to this Task. An epilog will prevent this task from stopping until it's completed.<br>
	 * An epilog can be any instance of CompletableFuture.
	 * @param epilog an instance of CompletableFuture
	 */
	public void registerEpilog(CompletableFuture<?> epilog) {
		epilogs.add(epilog);
		epilog.handle((r, f) ->{
			// just wakeup the task so the epilog completion will be taken into account
			wakeup();
			return null;
		});
	}
	
	/**
	 * Require this task to stop.
	 */
	public void stopTask() {
		logger.fatal("{} {}", this, new LogFmt().append("event", "STOP REQUESTED"));
		run = false;
		wakeup();
	}

	@Override
	public void run() {
		try {
			logger.always().log("{} {}", this, new LogFmt().append("event", "STARTING"));
			this.internalEventStartTask();

			// avoid sleeping if jobs are already available
			if (jobs.size() > 0)
				ecb.post();

			while (isActive()) { // isActive(): run || ! epilogs.isEmpty()
				try {
					// wait until we get some messages
					ecb.waitForPost(500);

					// Trigger the stop event once
					if(run == false && stopEventTriggered == false) {
						logger.always().log("{} {}", this, new LogFmt().append("event", "STARTING STOP SEQUENCE"));
						stopEventTriggered = true;
						this.internalEventStopRequested();
					}
					
					Runnable wJob;
					while ((wJob = jobs.poll()) != null) {
						try {
							wJob.run();
						} catch (Throwable e) {
							logger.error("Unexpected exception while executing event.", e);
						}
					}
					
					// remove all epilogs which are done
					Iterator<CompletableFuture<?>> it = epilogs.iterator();
					while(it.hasNext()) {
						CompletableFuture<?> epilog = it.next();
						if(epilog.isDone()) {
							epilogs.remove(epilog);
						}
					}

				} catch (InterruptedException e) {
					logger.error("Task interrupted", e);
				}
				
				// last time a full iteration has been performed
				lastScanTime = System.currentTimeMillis();
			}
			
			if(terminals.size() > 0) {
				@SuppressWarnings("unchecked")
				Iterator<Terminal<?>> it = ((ArrayList<Terminal<?>>)terminals.clone()).iterator();
				while(it.hasNext()) {
					Terminal<?> t = it.next();
					logger.fatal("Terminating : {}", t);
					t.terminate();
				}
			}
			
		} finally {
			logger.always().log("{} {}", this, new LogFmt().append("event", "END OF TASK"));
			this.appCtx.removeTask(this);
		}
	}

	private void internalEventStartTask() {
		// propagate event
		this.eventStartTask();
	}
	
	/**
	 * Event method called when this task starts to run on its Thread.<br>
	 * You can safely override this method to handle the event.
	 */
	public abstract void eventStartTask();
	
	private void internalEventStopRequested() {
		try {
			this.eventStopRequested();
		} catch (Throwable e) {
			logger.error("Unexpected exception while triggering stop event.", e);
		}
		
		Iterator<Terminal<?>> it = terminals.iterator();
		while(it.hasNext()) {
			try {
				it.next().eventStopRequested();
			} catch (Throwable e) {
				logger.error("Unexpected exception while triggering stop event.", e);
			}
		}
	}
	
	/**
	 * Event method called when the stop sequence of this task is triggered.<br>
	 * You can safely override this method to handle the event.<br>
	 * In order to prevent an immediate stop, you may add one or more epilogs to this Task by calling {@link #registerEpilog(CompletableFuture)}
	 */
	protected void eventStopRequested() {}
	
	/**
	 * Get the {@link ApplicationContext} of this Task;
	 * @return the ApplicationContext.
	 */
	public ApplicationContext getAppCtx() {
		return this.appCtx;
	}

	/**
	 * Get the Netty {@linkplain EventLoopGroup} used for networking within this {@link Task}.<br>
	 * The {@link EventLoopGroup} is provided by the {@linkplain ApplicationContext}.
	 * @return Netty's EventLoopGroup
	 */
	public EventLoopGroup getEventloopgroup() {
		return this.appCtx.getEventloopgroup();
	}
	
	/**
	 * Get the {@linkplain Task#lastScanTime} for this {@link Task}.<br>
	 * See javadoc of {@linkplain Task#lastScanTime} for details.
	 * @return A timestamp in milliseconds
	 */
	public long getLastScanTime() {
		return lastScanTime;
	}
	
	@Override
	public String toString() {
		return new LogFmt()
				.append("type", getClass().getSimpleName())
				.append("id", getName()).toString();
	}
	
	//#region
	//***************************************************************************
	// Netty sockets management
	//***************************************************************************
	
	/**
	 * Synchronously open a server socket (Netty)
	 * @param inetHost host as a name or IP
	 * @param inetPort TCP port
	 * @param initializer Netty's ChannelInitializer
	 * @return the created ServerSocketChannel
	 * @throws InterruptedException if interrupted
	 */
	public ServerSocketChannel openServerSocket(String inetHost, int inetPort, ChannelInitializer<SocketChannel> initializer) throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
        b.group(getEventloopgroup())
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(initializer);
        
        return (ServerSocketChannel) b.bind(inetHost, inetPort).sync().channel();
	}
	
	/**
	 * Synchronously open a client socket (Netty)
	 * @param inetHost host as a name or IP
	 * @param inetPort TCP port
	 * @param initializer Netty's ChannelInitializer
	 * @return the created SocketChannel
	 * @throws InterruptedException if interrupted
	 */
	public SocketChannel openClientSocket(String inetHost, int inetPort, ChannelInitializer<SocketChannel> initializer) throws InterruptedException {
		Bootstrap b = new Bootstrap();
		b.group(getEventloopgroup())
			.channel(NioSocketChannel.class)
			//.handler(new LoggingHandler(LogLevel.INFO))
			.handler(initializer);
		
		return (SocketChannel) b.connect(inetHost, inetPort).sync().channel();
	}

	//#endregion
	
	//#region
	//***************************************************************************
	// Future management
	//***************************************************************************
	
	/**
	 * Listen for future completion and call the given method.
	 * @param <T> The future type
	 * @param future the listened future
	 * @param method the method called on completion
	 */
	public  <T> void listenFuture(Future<T> future, VoidFunction1<Future<T>> method) {
		future.addListener((f) -> Async.runLater(this, method, future));
	}
	
	/**
	 * Listen for future completion and call the given method.
	 * @param future the listened future
	 * @param method the method called on completion
	 */
	public void listenFuture(ChannelFuture future, VoidFunction1<ChannelFuture> method) {
		future.addListener((f) -> Async.runLater(this, method, future));
	}
	
	/**
	 * Listen for future completion and call the given method.
	 * @param <T> The future type
	 * @param future the listened future
	 * @param method the method called on completion
	 */
	public  <T> void listenFuture(CompletableFuture<T> future, VoidFunction1<CompletableFuture<T>> method) {
		future.handle((r, t) -> Async.runLater(this, method, future));
	}

	//#endregion
	
	//#region
	
	/**
	 * Execute the given command at some time in the future using the Thread of this Task.<br>
	 * Implementation of {@link Executor}
	 */
	public void execute(Runnable e) {
		jobs.add(e);
		this.wakeup();
	}
	
	/**
	 * Implementation of {@linkplain Async#callBefore(Executor, TimeLimit, Callable)} using the executor of this task.<br>
	 * 
	 * @param <R> The return Type
	 * @param limit The TimeLimit for this call.
	 * @param callable The Callable to execute.
	 * @return a Completable future.
	 */
	public <R> CompletableFuture<R> callBefore(TimeLimit limit, Callable<R> callable){
		return Async.callBefore(this, limit, callable);
	}
	
	@Override
	public boolean isActive() {
		return (run || ! epilogs.isEmpty());
	}
	
	//***************************************************************************
	// Short calls to Async static methods on this Executor.
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
		return Async.newTimeout(this, delay, unit, t -> {
			runLater(task::run, t);
		});
	}
	
	
	//#endregion
}
