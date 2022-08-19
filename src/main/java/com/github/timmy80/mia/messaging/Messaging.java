package com.github.timmy80.mia.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.timmy80.mia.core.Async;
import com.github.timmy80.mia.core.Task;
import com.github.timmy80.mia.core.TimeLimit;
import com.github.timmy80.mia.messaging.MessageCtx.Type;

import io.prometheus.client.Counter;
import io.prometheus.client.Counter.Child;

/**
 * Topic based messaging system.<br>
 * The topics are working as the MQTT protocol topics.<br>
 * It supports Question/Response and Push messaging patterns.
 * @author anthony
 *
 */
public class Messaging<Q, R> {

	// prometheus monitoring
	//private static final Summary topicCallLatency = Summary.build().name("topic_call_latency_seconds").help("Topic call latency in seconds.").labelNames("task", "topic").register();
	private static final Counter topicCallCount = Counter.build("topic_call_count", "Counter on topic call").labelNames("task", "topic").register();
	private static final Counter messageCount = Counter.build("messages_total", "Total messages sent").labelNames("task", "type").register();
	
	/**
	 * Topic subscriptions map<topic, subscribers>.<br>
	 * This map is copied on write so write access is synchronized.<br>
	 * Concurrent read access does not require a lock but it means that subscriptions may not be up to date.
	 */
	private volatile HashMap<String, List<Subscription<Q>>> subsriptions = new HashMap<>(); // Copy on write pointer (synchronize only on write)
	
	/**
	 * Subscribe to a topic to receive messages.
	 * @param topicFilter A topic filter like specified by the MQTT protocol. See {@link TopicFilter} for details.
	 * @param task The subscribing task (The thread that will execute the message received event)
	 * @param subscriber The subscriber which implements the eventPublishReceid method.
	 */
	public void register(String topicFilter, Task task, Subscriber<Q> subscriber) {
		synchronized (subsriptions) {
			// Copy on Write
			@SuppressWarnings("unchecked")
			HashMap<String, List<Subscription<Q>>> wSubsriptions = (HashMap<String, List<Subscription<Q>>>) subsriptions.clone();
			
			List<Subscription<Q>> wSubscribers = wSubsriptions.get(topicFilter);
			if (wSubscribers == null) {
				wSubscribers = new ArrayList<>();
				wSubsriptions.put(topicFilter, wSubscribers);
			}

			try {
				wSubscribers.add(new Subscription<Q>(topicFilter, task, subscriber));
			} catch (InvalidTopicFilterException e) {
				// Impossible
			}
			
			// Update Copy on write pointer
			subsriptions = wSubsriptions;
		}
	}
	
	public void unregister(String topicFilter, Task task, Subscriber<Q> subscriber) {
		synchronized (subsriptions) {
			// Copy on Write
			@SuppressWarnings("unchecked")
			HashMap<String, List<Subscription<Q>>> wSubsriptions = (HashMap<String, List<Subscription<Q>>>) subsriptions.clone();
			
			List<Subscription<Q>> wSubscribers = wSubsriptions.get(topicFilter);
			List<Subscription<Q>> wNewSubscribers = null;
			
			if(wSubscribers != null) {
				for(Subscription<Q> sub : wSubscribers) {
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
	 * 
	 * @param <R> The Response payload type
	 * @param <T> The Request payload type
	 * @param context The messaging context
	 * @param message The request payload
	 * @param publisher The publishing task
	 * @return
	 */
	private MessageCtx<R> publish(MessageCtx<R> context, Q message, Task publisher){
		List<Subscription<Q>> wSubscription = new ArrayList<Subscription<Q>>();
		for(List<Subscription<Q>> sub : subsriptions.values()) {
			if(sub.get(0).matches(context.getTopic()))
				wSubscription.addAll(sub);
		}
		
		if(wSubscription.isEmpty()) {
			throw new InvalidTopicException(String.format("No match for topic: ", context.getTopic()));
		}
		
		Child promCount = topicCallCount.labels(publisher.getName(), context.getTopic());
		
		Subscription<Q> wDest = wSubscription.get((int) (promCount.get()%wSubscription.size()));
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
	
	public MessageCtx<R> publish(String topic, Q message, TimeLimit limit, Task publisher) { // push
		MessageCtx<R> context = new MessageCtx<R>(topic, publisher, limit);
		return publish(context, message, publisher);
	}
	
	public MessageCtx<R> publish(String topic, Q message, TimeLimit limit, ResponseHandler<R> responseHandler, Task publisher){ // Q<T> -> ResponseHandler<R>
		MessageCtx<R> context = new MessageCtx<R>(topic, publisher, limit, responseHandler);
		return publish(context, message, publisher);
	}
}
