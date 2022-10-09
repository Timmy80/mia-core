package com.github.timmy80.mia.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * A common context for multiple {@link Task}<br>
 * Ex : A call on {@linkplain ApplicationContext#stop()} will perform a stop on all the {@link Task} of this context.
 * @author anthony
 *
 */
public class ApplicationContext {

	private static Logger logger = LogManager.getLogger(ApplicationContext.class);
	private static ApplicationContext defaultAppCtx = null;
	
	/**
	 * Get or Allocate the default {@link ApplicationContext} using the default parameters.
	 * @return the default {@link ApplicationContext}
	 */
	public static ApplicationContext getDefault() {
		if(defaultAppCtx == null) {
			defaultAppCtx = new ApplicationContext();
		}
		
		return defaultAppCtx;
	}
	
	private final ApplicationContextParams params;
	
	/**
	 * List of Tasks registered to this ApplicationContext.
	 */
	private final HashMap<String, Task> tasks = new HashMap<>();
	
	private AtomicBoolean stopPending = new AtomicBoolean(false); 
	
	/**
	 * The netty EventLoopGroup for this context.
	 */
	protected final EventLoopGroup eventLoopGroup;

	/**
	 * Create an ApplicationContext with customized parameter.
	 * @param params This context parameters. Will be cloned, so you wont be able to change them afterwards.
	 */
	protected ApplicationContext(ApplicationContextParams params) {
		eventLoopGroup = (params.getNetThreads() == null)?new NioEventLoopGroup():new NioEventLoopGroup(params.getNetThreads());
		this.params = (ApplicationContextParams) params.clone();
		ApplicationContext appCtx = this;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			appCtx.stop();
			appCtx.join();
		}));
	}
	
	/**
	 * Create an ApplicationContext with the default parameters.
	 */
	protected ApplicationContext() {
		this(new ApplicationContextParams());
	}
	
	/**
	 * Register a task to this ApplicationContext.<br>
	 * This method is called on task creation.
	 * @param task an implementation of {@link Task}
	 */
	protected void addTask(Task task) {
		synchronized (tasks) {
			if (tasks.containsKey(task.getName()))
				throw new IllegalArgumentException(String.format("Task name already reserved: %s", task.getName()));

			tasks.put(task.getName(), task);
		}
	}
	
	/**
	 * Remove a Task from this ApplicationContext.<br>
	 * This method is called when a task ends.
	 * @param task an implementation of {@link Task}
	 */
	protected void removeTask(Task task) {
		synchronized (tasks) {
			tasks.remove(task.getName());
			if(tasks.isEmpty() && isStopPending())
				eventLoopGroup.shutdownGracefully();
		}
	}
	
	/**
	 * Get the eventLoopGroup for Netty
	 * @return the EventLoopGroup of this ApplicationContext
	 */
	public EventLoopGroup getEventloopgroup() {
		return eventLoopGroup;
	}
	
	/**
	 * Get the host IP listened to for probes.
	 * @return An IP represented as a String
	 */
	public String getProbeInetHost() {
		return params.getProbeInetHost();
	}

	/**
	 * Get the port listened to for probes
	 * @return A TCP port number
	 */
	public int getProbeInetPort() {
		return params.getProbeInetPort();
	}
	
	/**
	 * Get the probe handlers.
	 * @return A Map where key=path, path=handler class.
	 */
	protected HashMap<String, Class<ProbeHandlerTerm>> getProbes() {
		return params.getProbes();
	}
	
	/**
	 * Get the stop sequence status
	 * @return True if stop sequence is pending.
	 */
	public boolean isStopPending() {
		return stopPending.get();
	}

	/**
	 * Provide a read only set on {@link Task} registered to this {@link ApplicationContext}
	 * @return A read only set of {@link Task}. Never null.
	 */
	public Set<Task> taskSet(){
		HashSet<Task> taskset = new HashSet<>();
		taskset.addAll(tasks.values());
		return Collections.unmodifiableSet(taskSet());
	}
	
	/**
	 * Check if all the tasks registered to this {@link ApplicationContext} have performed a full scan since at least timeMilliseconds.
	 * @param timeMilliseconds The time in milliseconds that a {@link Task} must not exceed for a fullScan.
	 * @return True if all the tasks are OK. False otherwise.
	 */
	public boolean watchdogCheck(long timeMilliseconds) {
		// iterate on all tasks
		Iterator<Task> it = tasks.values().iterator();
		while(it.hasNext()) {
			Task task = it.next();
			// if a task has not perform a full scan since now-timeMilliseconds
			if(task.getLastScanTime() < (System.currentTimeMillis() - timeMilliseconds)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Call {@link Task#stopTask()} on all the tasks registered to this ApplicationContext
	 */
	public void stop() {
		synchronized (tasks) {
			stopPending.set(true);
			for(Task task : tasks.values())
				task.stopTask();
		}
	}
	
	/**
	 * Join all the Tasks registered to this context.<br>
	 * This method is intended to be called after {@link #stop()}
	 */
	public void join() {
		HashMap<String, Task> wTasks = null;
		synchronized (tasks) {
			wTasks = new HashMap<String, Task>(tasks);
		}
		
		for(Task task : wTasks.values()) {
			try {
				task.join();
			} catch (InterruptedException e) {
				logger.error("Interrupted join", e);
			}
		}
		if(eventLoopGroup.isShuttingDown())
			eventLoopGroup.terminationFuture().awaitUninterruptibly();
	}
}
