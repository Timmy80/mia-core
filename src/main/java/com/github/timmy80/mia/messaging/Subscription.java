package com.github.timmy80.mia.messaging;

import com.github.timmy80.mia.core.Task;

/**
 * A messaging topic subscription.<br>
 * Associate a {@link Subscriber} to a {@link TopicFilter}
 * @author anthony
 *
 * @param <Q> The type of the Question messages used by the subscriber
 */
public class Subscription<Q> {
	
	private final TopicFilter topicFilter;
	private final Task handlingTask;
	private final Subscriber<Q> subscriber;
	
	/**
	 * Consctructor
	 * @param topicFilter The topic filter subscribed to
	 * @param handlingTask The task of the subscriber
	 * @param subscriber The subscriber
	 * @throws InvalidTopicFilterException If the topicFilter is not a valid topic filter
	 */
	public Subscription(String topicFilter, Task handlingTask, Subscriber<Q> subscriber) throws InvalidTopicFilterException {
		this.topicFilter = new TopicFilter(topicFilter);
		this.handlingTask = handlingTask;
		this.subscriber = subscriber;
	}
	
	/**
	 * Check if the given topic matches this filter
	 * @param topic a topic string to match
	 * @return true on match
	 */
	public boolean matches(String topic) {
		return topicFilter.matches(topic);
	}

	/**
	 * Get the task of the subscriber
	 * @return a Task
	 */
	public Task getHandlingTask() {
		return handlingTask;
	}

	/**
	 * Get the subscriber
	 * @return a Subscriber
	 */
	public Subscriber<Q> getSubscriber() {
		return subscriber;
	}

}
