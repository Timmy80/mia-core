package com.github.timmy80.mia.core;

import java.util.HashMap;
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
import io.netty.channel.nio.NioEventLoopGroup;
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
	private static final HashMap<String, Task> tasks = new HashMap<>();
	private static final Messaging messaging = new Messaging();
	
	protected static Integer netThreads = null; 
	protected static EventLoopGroup eventLoopGroup;

	/**
	 * Keep this to true in order to keep this thread alive.
	 */
	private boolean run = true;
	
	//private static final Summary topicCallLatency = Summary.build().name("topic_call_latency_seconds").help("Topic call latency in seconds.").labelNames("task", "topic").register();
	
	ConcurrentLinkedQueue<Runnable> jobs = new ConcurrentLinkedQueue<>();

	EventControlBlock ecb = new EventControlBlock();

	public Task(String name) throws IllegalArgumentException {
		super(name);
		synchronized (tasks) {
			if (tasks.containsKey(name))
				throw new IllegalArgumentException(String.format("Task name %s is already reserved", name));

			tasks.put(name, this);
		}
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
		if(topicFilter.startsWith("task/"))
			throw new InvalidTopicException(String.format("topic starting with 'task' are reserved for system usage", topicFilter));
		
		messaging.register(topicFilter, this, subscriber);
	}
	
	/**
	 * Subscribe to a topicFilter
	 * @param topicFilter
	 * @throws InvalidTopicException if the topic start with "/task/".
	 */
	public <T> void unsubscribe(String topicFilter, Subscriber<T> subscriber) {
		if(topicFilter.startsWith("task/"))
			throw new InvalidTopicException(String.format("topic starting with 'task' are reserved for system usage", topicFilter));
		
		messaging.unregister(topicFilter, this, subscriber);
	}

	/**
	 * Public a message using it's topic.<br>
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
		return messaging.publish(topic, payload, limit, responseHandler, this);
	}
	
	public <R, T> MessageCtx<Void> publish(String topic, T payload, TimeLimit limit) {
		return messaging.publish(topic, payload, limit, this);
	}

//	/**
//	 * Reply a message to it's publisher.
//	 * @param message the message to reply.
//	 */
//	public void reply(Message message) {
//		messaging.reply(message);
//	}
	
	public void stopTask() {
		logger.fatal("STOP REQUESTED");
		run = false;
		wakeup();
	}

	@Override
	public void run() {
		try {
			this.internalEventStartTask();

			// avoid sleeping if jobs are already available
			if (jobs.size() > 0)
				ecb.post();

			while (run) {
				try {
					// wait until we get some messages
					ecb.waitForPost(500);
					
					Runnable wJob;
					while ((wJob = jobs.poll()) != null) {
						try {
							wJob.run();
						} catch (Throwable e) {
							logger.error("Unexpected exception while executing event.", e);
						}
					}

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} finally {
			logger.info("End of task: {}", getName());
		}
	}


	private void internalEventStartTask() {
		//subscribe to internal topics
		messaging.register(String.format("task/%s/stop", getName()), this, (message, payload)-> this.internalEventStopRequested());
		
		// propagate event
		this.eventStartTask();
	}
	
	public abstract void eventStartTask();
	
	protected void internalEventStopRequested() {
	}
	
//	@SuppressWarnings("rawtypes")
//	public void eventEndOfTerminal(Terminal term) {};

	public static EventLoopGroup getEventloopgroup() {
		if(eventLoopGroup != null)
			return eventLoopGroup;
		
		synchronized (tasks) {
			if(eventLoopGroup == null)
				eventLoopGroup = (netThreads == null)?new NioEventLoopGroup():new NioEventLoopGroup(netThreads);
		}
		
		return eventLoopGroup;
	}
	
	public static Integer getNetThreads() {
		return netThreads;
	}

	public static void setNetThreads(Integer netThreads) {
		if(eventLoopGroup != null)
			throw new IllegalStateException("Cannot set the number of threads after the creation of the eventLoopGroup");
		
		Task.netThreads = netThreads;
	}
	
	//#region
	//***************************************************************************
	// Netty sockets management
	//***************************************************************************
	
	public ServerSocketChannel openServerSocket(String inetHost, int inetPort, ChannelInitializer<SocketChannel> initializer) throws InterruptedException {
		ServerBootstrap b = new ServerBootstrap();
        b.group(Task.getEventloopgroup())
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(initializer);
        
        return (ServerSocketChannel) b.bind(inetHost, inetPort).sync().channel();
	}
	
	public SocketChannel openClientSocket(String inetHost, int inetPort, ChannelInitializer<SocketChannel> initializer) throws InterruptedException {
		Bootstrap b = new Bootstrap();
		b.group(Task.getEventloopgroup())
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
	//***************************************************************************
	// Short calls to Async static methods on this Executor.
	//***************************************************************************
	
	/**
	 * Execute the given command at some time in the future using the Thread of this Task.<br>
	 * Implementation of {@link Executor}
	 */
	public void execute(Runnable e) {
		jobs.add(e);
		this.wakeup();
	}
	
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
	
	public Timeout newTimeout(long delayms, TimerTask task) {
		return Async.newTimeout(this, delayms, task);
	}
	
	public Timeout newTimeout(long delay, TimeUnit unit, TimerTask task) {
		return Async.newTimeout(this, delay, unit, task);
	}
	
	
	//#endregion
}
