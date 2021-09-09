package com.github.timmy80.mia.core;

import java.util.regex.Pattern;

/**
 * A topic filter that follows MQTT topic rules.
 * @author anthony
 *
 */
public class TopicFilter {
	
	/**
	 * A Regular Expression to match against topic levels to check if they respect the required format for a topic filter.
	 */
	public static final Pattern TOPIC_PART_REGEX = Pattern.compile("^([+#]{1}|[^/+#]*)$");
	
	/**
	 * The topic filter as received by the constructor.
	 */
	private final String topicFilter;
	
	/**
	 * The RegularExpression to match against real topics
	 */
    private final Pattern topicRegex;
	
	public TopicFilter(String topicFilter) throws InvalidTopicFilterException {
		for(String token : topicFilter.split("/")) {
			if(!TOPIC_PART_REGEX.matcher(token).matches())
				throw new InvalidTopicFilterException(String.format("Invalid topic filter '%s'. Topic level '%s' does not match regex: %s", topicFilter, token, TopicFilter.TOPIC_PART_REGEX));
		}
		
		this.topicFilter = topicFilter;
		this.topicRegex=Pattern.compile(topicFilter.replace(".","\\.").replace("*", "\\*").replace("+", "([^/]+)").replace("#", "(.+)") + "$");
	}
	
	public String toString() {
		return topicFilter;
	}
	
	public Pattern topicRegex() {
		return topicRegex;
	}
	
    public boolean matches(String topic){
        return this.topicRegex.matcher(topic).matches();
    }

}
