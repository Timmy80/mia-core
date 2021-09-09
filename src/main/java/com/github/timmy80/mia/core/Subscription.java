package com.github.timmy80.mia.core;

@SuppressWarnings("rawtypes")
public class Subscription {
	
	private final TopicFilter topicFilter;
	private final Task handlingTask;
	private final Subscriber subscriber;
	
	public Subscription(String topicFilter, Task handlingTask, Subscriber subscriber) throws InvalidTopicFilterException {
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

	public Subscriber getSubscriber() {
		return subscriber;
	}

}
