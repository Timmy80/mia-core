package com.github.timmy80.mia.messaging;

import com.github.timmy80.mia.core.Task;
import com.github.timmy80.mia.core.TimeLimit;

public class MessagingTask<Q,R> extends Task {
	
	private final Messaging<Q,R> messaging;

	public MessagingTask(String name, Messaging<Q,R> messaging) throws IllegalArgumentException {
		super(name);
		this.messaging = messaging;
	}

	@Override
	public void eventStartTask() {
		// TODO Auto-generated method stub
		
	}

	public Messaging<Q,R> getMessaging() {
		return messaging;
	}
	
	/**
	 * Subscribe to a topicFilter
	 * @param topicFilter
	 * @throws InvalidTopicException if the topic start with "/task/".
	 */
	public void subscribe(String topicFilter, Subscriber<Q> subscriber) {	
		getMessaging().register(topicFilter, this, subscriber);
	}
	
	/**
	 * Subscribe to a topicFilter
	 * @param topicFilter
	 * @throws InvalidTopicException if the topic start with "/task/".
	 */
	public void unsubscribe(String topicFilter, Subscriber<Q> subscriber) {
		getMessaging().unregister(topicFilter, this, subscriber);
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
	public MessageCtx<R> publish(String topic, Q payload, TimeLimit limit, ResponseHandler<R> responseHandler) {
		return getMessaging().publish(topic, payload, limit, responseHandler, this);
	}
	
	public MessageCtx<R> publish(String topic, Q payload, TimeLimit limit) {
		return getMessaging().publish(topic, payload, limit, this);
	}
	
}
