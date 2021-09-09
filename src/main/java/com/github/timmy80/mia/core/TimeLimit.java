package com.github.timmy80.mia.core;

import java.util.concurrent.TimeUnit;

/**
 * A simple expression of a time limit used for real time applications.<br>
 * A TimeLimit less or equal to 0 means no limit.
 * @author anthony
 *
 */
public class TimeLimit {

	private final long before;
	
	public TimeLimit(long before) {
		this.before = before;
	}

	public long getNotAfter() {
		return before;
	}
	
	public long remaining() {
		return before - System.currentTimeMillis();
	}
	
	public boolean isExpired() {
		if(isNoLimit()) // means no limit
			return false;
		
		return (before <= System.currentTimeMillis());
	}
	
	public boolean isNoLimit() {
		return (before <= 0); // means no limit
	}
	
	public static TimeLimit in(long milliseconds) {
		// add 1ms to the give in expression. Just because "in" is interpreted like "not after" instead of before.
		return new TimeLimit(System.currentTimeMillis() + milliseconds+1);
	}
	
	public static TimeLimit in(long time, TimeUnit timeUnit) {
		// add 1ms to the give in expression. Just because "in" is interpreted like "not after" instead of before.
		return new TimeLimit(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(time, timeUnit)+1);
	}
	
	public static TimeLimit before(long timestamp) {
		return new TimeLimit(timestamp);
	}
	
	public static TimeLimit noLimit() {
		return new TimeLimit(0L);
	}
}
