package com.github.timmy80.mia.messaging;

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
	
    /**
     * Constructor
     * @param topicFilter The topic filter string
     * @throws InvalidTopicFilterException If the String is not a valid topic filter
     */
	public TopicFilter(String topicFilter) throws InvalidTopicFilterException {
		for(String token : topicFilter.split("/")) {
			if(!TOPIC_PART_REGEX.matcher(token).matches())
				throw new InvalidTopicFilterException(String.format("Invalid topic filter '%s'. Topic level '%s' does not match regex: %s", topicFilter, token, TopicFilter.TOPIC_PART_REGEX));
		}
		
		this.topicFilter = topicFilter;
		this.topicRegex=Pattern.compile(topicFilter.replace(".","\\.").replace("*", "\\*").replace("+", "([^/]+)").replace("#", "(.+)") + "$");
	}
	
	@Override
	public String toString() {
		return topicFilter;
	}
	
	/**
	 * Get the topic filter as a regex
	 * @return a regex for this topic filter
	 */
	public Pattern topicRegex() {
		return topicRegex;
	}
	
	/**
	 * Check if the given topic matches this filter
	 * @param topic a topic string to match
	 * @return true on match
	 */
    public boolean matches(String topic){
        return this.topicRegex.matcher(topic).matches();
    }

}
