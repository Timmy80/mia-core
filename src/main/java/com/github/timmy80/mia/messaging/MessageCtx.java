package com.github.timmy80.mia.messaging;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import com.github.timmy80.mia.core.Async;
import com.github.timmy80.mia.core.Task;
import com.github.timmy80.mia.core.TimeLimit;

public class MessageCtx<R extends Object> {
	public static enum Type {
		REQUEST,
		PUSH
	}

	private final Type type;
	private final String topic;
	private final Task publisher;
	private final TimeLimit limit;
	private final ResponseHandler<R> responseHandler;
	protected final CompletableFuture<Void> responseFuture = new CompletableFuture<Void>();
	
	private long publishTime = 0L;
	private Task handler = null;
	private CompletableFuture<Void> completionFuture = null;
	
	public MessageCtx(String topic, Task publisher, TimeLimit limit) {
		this(topic, publisher, limit, null);
	}
	
	public MessageCtx(String topic, Task publisher, TimeLimit limit, ResponseHandler<R> responseHandler) {
		this.type = (responseHandler==null)?Type.PUSH:Type.REQUEST;
		this.topic = topic;
		this.publisher = publisher;
		this.limit = limit;
		this.responseHandler = responseHandler;
	}

	public Task getPublisher() {
		return publisher;
	}

	public long getPublishTime() {
		return publishTime;
	}

	protected void setPublishTime(long publishTime) {
		this.publishTime = publishTime;
	}

	public Task getHandler() {
		return handler;
	}
	
	protected void setHandler(Task handler) {
		this.handler = handler;
	}

	protected ResponseHandler<R> getResponseHandler() {
		return responseHandler;
	}

	public Type getType() {
		return type;
	}

	public String getTopic() {
		return topic;
	}

	public TimeLimit getLimit() {
		return limit;
	}

	public CompletableFuture<Void> getCompletionFuture() {
		return completionFuture;
	}

	protected void setCompletionFuture(CompletableFuture<Void> completionFuture) {
		this.completionFuture = completionFuture;
	}
	
	public boolean isDone() {
		if(completionFuture == null)
			return false;
		else
			return completionFuture.isDone();
	}
	
	public void join() throws InterruptedException, ExecutionException {
		if(completionFuture != null)
			completionFuture.get();
	}
	
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
