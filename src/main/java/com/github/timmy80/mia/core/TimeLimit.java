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
	
	/**
	 * Constructor
	 * @param before the limit timestamp in milliseconds
	 */
	public TimeLimit(long before) {
		this.before = before;
	}

	/**
	 * Get the limit timestamp in milliseconds
	 * @return a timestamp in milliseconds
	 */
	public long getNotAfter() {
		return before;
	}
	
	/**
	 * Get the remaining tume in milliseconds
	 * @return A timestamp in milliseconds. May be negative it this limit has been crossed.
	 */
	public long remaining() {
		return before - System.currentTimeMillis();
	}
	
	/**
	 * Get if this limit is crossed
	 * @return True if the limit is crossed
	 */
	public boolean isExpired() {
		if(isNoLimit()) // means no limit
			return false;
		
		return (before <= System.currentTimeMillis());
	}
	
	/**
	 * Check if this a No limit (expire at timestamp 0)
	 * @return True if this a no limit
	 */
	public boolean isNoLimit() {
		return (before <= 0); // means no limit
	}
	
	/**
	 * Create {@link TimeLimit} which expires in the given milliseconds
	 * @param milliseconds a time in milliseconds
	 * @return a {@link TimeLimit}
	 */
	public static TimeLimit in(long milliseconds) {
		// add 1ms to the give in expression. Just because "in" is interpreted like "not after" instead of before.
		return new TimeLimit(System.currentTimeMillis() + milliseconds+1);
	}
	
	/**
	 * Create {@link TimeLimit} which expires in the given time
	 * @param time a quantity of time
	 * @param timeUnit a unit for the time
	 * @return a {@link TimeLimit}
	 */
	public static TimeLimit in(long time, TimeUnit timeUnit) {
		// add 1ms to the give in expression. Just because "in" is interpreted like "not after" instead of before.
		return new TimeLimit(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(time, timeUnit)+1);
	}
	
	/**
	 * Create {@link TimeLimit} which expires a the give timestamp
	 * @param timestamp a timestamp in milliseconds
	 * @return a {@link TimeLimit}
	 */
	public static TimeLimit before(long timestamp) {
		return new TimeLimit(timestamp);
	}
	
	/**
	 * Create a specific TimeLimit which never expires (timestamp = 0)
	 * @return a {@link TimeLimit}
	 */
	public static TimeLimit noLimit() {
		return new TimeLimit(0L);
	}
}
