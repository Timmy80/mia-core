package com.github.timmy80.mia.core;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.timmy80.mia.messaging.InvalidTopicFilterException;
import com.github.timmy80.mia.messaging.TopicFilter;

public class TopicFilterTest {
	
	@Test(expected = InvalidTopicFilterException.class)
	public void testInvalidPattern1() throws InvalidTopicFilterException {
		new TopicFilter("home+");
	}	
	
	@Test(expected = InvalidTopicFilterException.class)
	public void testInvalidPattern2() throws InvalidTopicFilterException {
		new TopicFilter("home/foo#");
	}
	
	@Test
	public void tests() throws InvalidTopicFilterException {
		test("/", "toto/", false);
		test("/", "/", true);
		test("/toto", "toto", false);
		test("/toto.*", "/toto1", false);
		test("/toto.*", "/toto.*", true);
		test("home/#/top-light", "home/upstairs/parental-room/top-light", true);
		test("home/+/top-light", "home/upstairs/parental-room/top-light", false);
		test("home/+/top-light", "home/garage/top-light", true);
	}
	
    public static void test(String filter, String topic, boolean expected) throws InvalidTopicFilterException {
    	TopicFilter reg = new TopicFilter(filter);
    	boolean actual = reg.matches(topic);
    	System.out.println(String.format("[%s] [%s] %-20s %-25s %-20s", (actual == expected)?"OK":"KO", (actual)?" MATCH ":"UNMATCH", reg, reg.topicRegex(), topic));
    	assertEquals(expected, actual);
    }

}
