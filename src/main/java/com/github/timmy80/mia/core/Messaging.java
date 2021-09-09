package com.github.timmy80.mia.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.timmy80.mia.core.MessageCtx.Type;

import io.prometheus.client.Counter;
import io.prometheus.client.Counter.Child;

/**
 * Topic based messaging system.<br>
 * The topics are working as the MQTT protocol topics.<br>
 * It supports Question/Response and Push messaging patterns.
 * @author anthony
 *
 */
public class Messaging {

	// prometheus monitoring
	//private static final Summary topicCallLatency = Summary.build().name("topic_call_latency_seconds").help("Topic call latency in seconds.").labelNames("task", "topic").register();
	private static final Counter topicCallCount = Counter.build("topic_call_count", "Counter on topic call").labelNames("task", "topic").register();
	private static final Counter messageCount = Counter.build("messages_total", "Total messages sent").labelNames("task", "type").register();
	
	private final HashMap<String, Task> tasks = new HashMap<>();
	private volatile HashMap<String, List<Subscription>> subsriptions = new HashMap<>(); // Copy on write pointer (synchronize only on write)
	
	/**
	 * Register a Task on the messaging system to allow internal communications for task management
	 * @param task
	 */
	protected void register(Task task) {
		synchronized (tasks) {
			tasks.put(task.getName(), task);
		}
		
		register(String.format("task/%s/stop", task.getName()), task, (message, payload)-> task.internalEventStopRequested());
	}
	
	/**
	 * Subscribe to a topic to receive messages.
	 * @param topicFilter A topic filter like specified by the MQTT protocol. See {@link TopicFilter} for details.
	 * @param task The subscribing task (The thread that will execute the message received event)
	 * @param subscriber The subscriber which implements the eventPublishReceid method.
	 */
	public void register(String topicFilter, Task task, Subscriber<?> subscriber) {
		synchronized (subsriptions) {
			// Copy on Write
			@SuppressWarnings("unchecked")
			HashMap<String, List<Subscription>> wSubsriptions = (HashMap<String, List<Subscription>>) subsriptions.clone();
			
			List<Subscription> wSubscribers = wSubsriptions.get(topicFilter);
			if (wSubscribers == null) {
				wSubscribers = new ArrayList<>();
				wSubsriptions.put(topicFilter, wSubscribers);
			}

			try {
				wSubscribers.add(new Subscription(topicFilter, task, subscriber));
			} catch (InvalidTopicFilterException e) {
				// Impossible
			}
			
			// Update Copy on write pointer
			subsriptions = wSubsriptions;
		}
	}
	
	public void unregister(String topicFilter, Task task, Subscriber<?> subscriber) {
		synchronized (subsriptions) {
			// Copy on Write
			@SuppressWarnings("unchecked")
			HashMap<String, List<Subscription>> wSubsriptions = (HashMap<String, List<Subscription>>) subsriptions.clone();
			
			List<Subscription> wSubscribers = wSubsriptions.get(topicFilter);
			List<Subscription> wNewSubscribers = null;
			
			if(wSubscribers != null) {
				for(Subscription sub : wSubscribers) {
					if(sub.getHandlingTask() != task || sub.getSubscriber() != subscriber) {
						if (wNewSubscribers == null)
							wNewSubscribers = new ArrayList<>();
						wNewSubscribers.add(sub);
					}
				}
			}
			
			if(wNewSubscribers == null)
				wSubsriptions.remove(topicFilter);
			else
				wSubsriptions.put(topicFilter, wNewSubscribers);
			
			// Update Copy on write pointer
			subsriptions = wSubsriptions;
		}
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
//	public <R, T> CompletableFuture<R> publish(String topic, T payload, Task publisher, ResponseHandler<R> responseHandler) {
//		MessageCtx<R> message = new MessageCtx<R>(Type.REQUEST, topic, publisher);
//		if(responseHandler == null)
//			throw new NullPointerException("responseHandler argument cannot be null");
//		message.setResponseHandler(responseHandler);
//		
//		List<Subscription> wSubscription = new ArrayList<Subscription>();
//		for(List<Subscription> sub : subsriptions.values()) {
//			if(sub.get(0).matches(message.getTopic()))
//				wSubscription.addAll(sub);
//		}
//		
//		if(wSubscription.isEmpty()) {
//			throw new InvalidTopicException(String.format("No match for topic: ", message.getTopic()));
//		}
//		
//		Child wCount = topicCallCount.labels(publisher.getName(), message.getTopic());
//		Subscription wDest = wSubscription.get((int) (wCount.get()%wSubscription.size()));
//		CompletableFuture<R> f = Async.callLater(wDest.getHandlingTask(), new Callable<R>() {
//			
//			@SuppressWarnings("unchecked")
//			@Override
//			public R call() throws Exception {
//				try {
//					wDest.getSubscriber().eventReceivePublish(message, payload);
//					return message.getResponse();
//				} catch(ClassCastException e) {
//					throw new InvalidPublishException(e);
//				}
//			}
//		});
//		wCount.inc();
//		messageCount.labels(publisher.getName(),message.getType().toString()).inc();
//		return f;
//	}
	
	/**
	 * 
	 * @param <R> The Response payload type
	 * @param <T> The Request payload type
	 * @param context The messaging context
	 * @param message The request payload
	 * @param publisher The publishing task
	 * @return
	 */
	private <R,T> MessageCtx<R> publish(MessageCtx<R> context, T message, Task publisher){
		List<Subscription> wSubscription = new ArrayList<Subscription>();
		for(List<Subscription> sub : subsriptions.values()) {
			if(sub.get(0).matches(context.getTopic()))
				wSubscription.addAll(sub);
		}
		
		if(wSubscription.isEmpty()) {
			throw new InvalidTopicException(String.format("No match for topic: ", context.getTopic()));
		}
		
		Child promCount = topicCallCount.labels(publisher.getName(), context.getTopic());
		
		Subscription wDest = wSubscription.get((int) (promCount.get()%wSubscription.size()));
		@SuppressWarnings("unchecked")
		CompletableFuture<Void> f = Async.runBefore(wDest.getHandlingTask(), context.getLimit(), wDest.getSubscriber()::eventReceivePublish, context, message);
		promCount.inc();
		messageCount.labels(publisher.getName(),context.getType().toString()).inc();
		if(context.getType() ==  Type.REQUEST) {
			f.exceptionally((e) -> {
				context.responseFuture.completeExceptionally(e);
				return null;
			});
			context.setCompletionFuture(CompletableFuture.allOf(f, context.responseFuture));
		}
		else {
			context.setCompletionFuture(f);
		}
		
		return context;
	}
	
	public <T> MessageCtx<Void> publish(String topic, T message, TimeLimit limit, Task publisher) { // push
		MessageCtx<Void> context = new MessageCtx<Void>(topic, publisher, limit);
		return publish(context, message, publisher);
	}
	
	public <T,R> MessageCtx<R> publish(String topic, T message, TimeLimit limit, ResponseHandler<R> responseHandler, Task publisher){ // Q<T> -> ResponseHandler<R>
		MessageCtx<R> context = new MessageCtx<R>(topic, publisher, limit, responseHandler);
		return publish(context, message, publisher);
	}
}
