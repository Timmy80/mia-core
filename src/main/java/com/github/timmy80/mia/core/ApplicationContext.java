package com.github.timmy80.mia.core;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.timmy80.mia.messaging.Messaging;

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
	 * @return
	 */
	public static ApplicationContext getDefault() {
		if(defaultAppCtx == null) {
			defaultAppCtx = new ApplicationContext();
		}
		
		return defaultAppCtx;
	}
	
	private final HashMap<String, Task> tasks = new HashMap<>();
	private final Messaging messaging = new Messaging();
	
	/**
	 * The netty EventLoopGroup for this context.
	 */
	protected final EventLoopGroup eventLoopGroup;

	/**
	 * Create an ApplicationContext with customized parameter.
	 * @param params
	 */
	protected ApplicationContext(ApplicationContextParams params) {
		eventLoopGroup = (params.getNetThreads() == null)?new NioEventLoopGroup():new NioEventLoopGroup(params.getNetThreads());
	}
	
	/**
	 * Create an ApplicationContext with the default parameter.
	 */
	protected ApplicationContext() {
		this(new ApplicationContextParams());
	}
	
	/**
	 * Register a task to this ApplicationContext.<br>
	 * This method is called on task creation.
	 * @param task
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
	 * @param task
	 */
	protected void removeTask(Task task) {
		synchronized (tasks) {
			tasks.remove(task.getName());
		}
	}
	
	/**
	 * Get the messaging context for this Application
	 * @return
	 */
	protected Messaging messaging() {
		return messaging;
	}
	
	/**
	 * Get the eventLoopGroup for Netty
	 * @return the EventLoopGroup of this ApplicationContext
	 */
	public EventLoopGroup getEventloopgroup() {
		return eventLoopGroup;
	}
	
	/**
	 * Call {@link Task#stopTask()} on all the tasks registered to this ApplicationContext
	 */
	public void stop() {
		synchronized (tasks) {
			for(Task task : tasks.values())
				task.stopTask();
		}
	}
	
	public void join() {
		synchronized (tasks) {
			for(Task task : tasks.values()) {
				try {
					task.join();
				} catch (InterruptedException e) {
					logger.error("Interrupted join", e);
				}
			}
		}
	}
}
