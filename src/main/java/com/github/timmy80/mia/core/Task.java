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
import com.github.timmy80.mia.messaging.InvalidTopicException;
import com.github.timmy80.mia.messaging.MessageCtx;
import com.github.timmy80.mia.messaging.ResponseHandler;
import com.github.timmy80.mia.messaging.Subscriber;

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

public abstract class Task extends Thread implements Executor {

	private static Logger logger = LogManager.getLogger(Task.class.getName());
	
	private ApplicationContext appCtx;

	/**
	 * Keep this to true in order to keep this thread alive.
	 */
	private boolean run = true;
	
	private boolean stopEventTriggered = false;
	
	//private static final Summary topicCallLatency = Summary.build().name("topic_call_latency_seconds").help("Topic call latency in seconds.").labelNames("task", "topic").register();
	
	ConcurrentLinkedQueue<Runnable> jobs = new ConcurrentLinkedQueue<>();
	ConcurrentLinkedQueue<CompletableFuture<?>> epilogs = new ConcurrentLinkedQueue<>();
	ArrayList<Terminal<?>> terminals = new ArrayList<>();

	EventControlBlock ecb = new EventControlBlock();

	/**
	 * Constructor to associate this task to an {@link ApplicationContext}
	 * @param name
	 * @param appCtx
	 * @throws IllegalArgumentException
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
	 * @throws IllegalArgumentException
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
	 * Subscribe to a topicFilter
	 * @param topicFilter
	 * @throws InvalidTopicException if the topic start with "/task/".
	 */
	public <T> void subscribe(String topicFilter, Subscriber<T> subscriber) {	
		appCtx.messaging().register(topicFilter, this, subscriber);
	}
	
	/**
	 * Subscribe to a topicFilter
	 * @param topicFilter
	 * @throws InvalidTopicException if the topic start with "/task/".
	 */
	public <T> void unsubscribe(String topicFilter, Subscriber<T> subscriber) {
		appCtx.messaging().unregister(topicFilter, this, subscriber);
	}

	/**
	 * Publish a message using it's topic.<br>
	 * If the topic is of the form "/task/taskName" the message will be published to the task "taskName".<br>
	 * Else the topic will be looked for into the topic list.<br>
	 * <br>
	 * When multiple subscribers are subscribed to a topic then the messages are load-balanced between the subscribers using the round-robin algorithm.
	 * @param message
	 * @param responseHandler the object called on response
	 * @return 
	 * @throws InvalidTopicException if the topic does not match any subscriber
	 * @throws NullPointerException if responseHandler is null
	 */
	public <R, T> MessageCtx<R> publish(String topic, T payload, TimeLimit limit, ResponseHandler<R> responseHandler) {
		return appCtx.messaging().publish(topic, payload, limit, responseHandler, this);
	}
	
	public <R, T> MessageCtx<Void> publish(String topic, T payload, TimeLimit limit) {
		return appCtx.messaging().publish(topic, payload, limit, this);
	}
	
	protected void registerTerminal(Terminal<?> terminal) {
		terminals.add(terminal);
	}
	
	protected void unRegisterTerminal(Terminal<?> terminal) {
		terminals.remove(terminal);
	}
	
	/**
	 * Add an epilog to this Task. An epilog will prevent this task from stopping until it's completed.<br>
	 * An epilog can be any instance of CompletableFuture.
	 * @param epilog
	 */
	public void registerEpilog(CompletableFuture<?> epilog) {
		epilogs.add(epilog);
		epilog.handle((r, f) ->{
			// just wakeup the task so the epilog completion will be taken into account
			wakeup();
			return null;
		});
	}
	
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

			while (run || ! epilogs.isEmpty()) {
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
	protected void eventStopRequested() {
	}
	
//	@SuppressWarnings("rawtypes")
//	public void eventEndOfTerminal(Terminal term) {};

	public EventLoopGroup getEventloopgroup() {
		return this.appCtx.getEventloopgroup();
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
	
	public ServerSocketChannel openServerSocket(String inetHost, int inetPort, ChannelInitializer<SocketChannel> initializer) throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
        b.group(getEventloopgroup())
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(initializer);
        
        return (ServerSocketChannel) b.bind(inetHost, inetPort).sync().channel();
	}
	
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
	
	public  <T> void listenFuture(Future<T> future, VoidFunction1<Future<T>> method) {
		future.addListener((f) -> Async.runLater(this, method, future));
	}
	
	public void listenFuture(ChannelFuture future, VoidFunction1<ChannelFuture> method) {
		future.addListener((f) -> Async.runLater(this, method, future));
	}
	
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
	
	//***************************************************************************
	// Short calls to Async static methods on this Executor.
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
