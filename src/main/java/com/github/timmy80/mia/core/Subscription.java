package com.github.timmy80.mia.core;

public class Subscription<Q> {
	
	private final TopicFilter topicFilter;
	private final Task handlingTask;
	private final Subscriber<Q> subscriber;
	
	public Subscription(String topicFilter, Task handlingTask, Subscriber<Q> subscriber) throws InvalidTopicFilterException {
		this.topicFilter = new TopicFilter(topicFilter);
		this.handlingTask = handlingTask;
		this.subscriber = subscriber;
	}
	
	public boolean matches(String topic) {
		return topicFilter.matches(topic);
	}

	public Task getHandlingTask() {
		return handlingTask;
	}

	public Subscriber<Q> getSubscriber() {
		return subscriber;
	}

}
