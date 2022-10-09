package com.github.timmy80.mia.messaging;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import com.github.timmy80.mia.core.Async;
import com.github.timmy80.mia.core.Task;
import com.github.timmy80.mia.core.TimeLimit;

import io.netty.util.internal.UnstableApi;

/**
 * A message context
 * @author anthony
 *
 * @param <R> The Response type
 */
@UnstableApi
public class MessageCtx<R extends Object> {
	/**
	 * Type of request
	 * @author anthony
	 *
	 */
	public static enum Type {
		/**
		 * a response is expected by the send
		 */
		REQUEST,
		/**
		 * No response if expected by the sender
		 */
		PUSH
	}

	private final Type type;
	private final String topic;
	private final Task publisher;
	private final TimeLimit limit;
	private final ResponseHandler<R> responseHandler;
	
	/**
	 * A future completed when the response is given
	 */
	protected final CompletableFuture<Void> responseFuture = new CompletableFuture<Void>();
	
	private long publishTime = 0L;
	private Task handler = null;
	private CompletableFuture<Void> completionFuture = null;
	
	/**
	 * Constructor for a push (no response expected)
	 * @param topic the requested topic
	 * @param publisher The publishing task
	 * @param limit The {@link TimeLimit} to handle the request
	 */
	public MessageCtx(String topic, Task publisher, TimeLimit limit) {
		this(topic, publisher, limit, null);
	}
	
	/**
	 * Constructor for a Question/Response constext
	 * @param topic  the requested topic
	 * @param publisher The publishing task
	 * @param limit The {@link TimeLimit} to handle the request
	 * @param responseHandler The response handler
	 */
	public MessageCtx(String topic, Task publisher, TimeLimit limit, ResponseHandler<R> responseHandler) {
		this.type = (responseHandler==null)?Type.PUSH:Type.REQUEST;
		this.topic = topic;
		this.publisher = publisher;
		this.limit = limit;
		this.responseHandler = responseHandler;
	}

	/**
	 * Get the publishing Task
	 * @return a task
	 */
	public Task getPublisher() {
		return publisher;
	}

	/**
	 * Get the publishing time
	 * @return a timestamp in milliseconds
	 */
	public long getPublishTime() {
		return publishTime;
	}

	/**
	 * Set the publishing time
	 * @param publishTime a timestamp in milliseconds
	 */
	protected void setPublishTime(long publishTime) {
		this.publishTime = publishTime;
	}

	/**
	 * Unused
	 * @return null
	 */
	@UnstableApi
	public Task getHandler() {
		return handler;
	}
	
	/**
	 * unused 
	 * @param handler unused
	 */
	@UnstableApi
	protected void setHandler(Task handler) {
		this.handler = handler;
	}

	/**
	 * Get the ResponseHandler
	 * @return may be null in case of a PUSH
	 */
	protected ResponseHandler<R> getResponseHandler() {
		return responseHandler;
	}

	/**
	 * Get the context type
	 * @return PUSH or REQUEST
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Get the requested topic
	 * @return a topic as a string
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Get the {@link TimeLimit} for this request
	 * @return a TimeLimit
	 */
	public TimeLimit getLimit() {
		return limit;
	}

	/**
	 * Get the completion future
	 * @return a CompletableFuture or null if not set
	 */
	public CompletableFuture<Void> getCompletionFuture() {
		return completionFuture;
	}

	/**
	 * Set the completion future
	 * @param completionFuture a void CompletableFuture
	 */
	protected void setCompletionFuture(CompletableFuture<Void> completionFuture) {
		this.completionFuture = completionFuture;
	}
	
	/**
	 * Check if completionFuture isDone()
	 * @return completionFuture.isDone()
	 */
	public boolean isDone() {
		if(completionFuture == null)
			return false;
		else
			return completionFuture.isDone();
	}
	
	/**
	 * Wait for completionFuture to be completed
	 * @throws InterruptedException see {@link CompletableFuture#get()}
	 * @throws ExecutionException see {@link CompletableFuture#get()}
	 */
	public void join() throws InterruptedException, ExecutionException {
		if(completionFuture != null)
			completionFuture.get();
	}
	
	/**
	 * Reply to the request
	 * @param response the response object
	 */
	@SuppressWarnings("unchecked")
	public void reply(Object response) {
		if(type == Type.REQUEST)
			Async.runBefore(publisher, getLimit(), responseHandler::eventResponseReceived, this, (R)response).handle(new BiFunction<Void, Throwable, Void>() {

				@Override
				public Void apply(Void t, Throwable u) {
					if(u != null)
						responseFuture.completeExceptionally(u);
					else
						responseFuture.complete(null);
					return null;
				}
			});
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Message type ")
			   .append(getType())
			   .append(" on topic ")
			   .append(getTopic())
			   .append("\n");
		return builder.toString();
	}
}
