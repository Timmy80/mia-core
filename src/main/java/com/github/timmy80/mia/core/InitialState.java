package com.github.timmy80.mia.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Initial dumb State which is used as the default state of Terminal on terminal creation.
 * @author anthony
 *
 */
public class InitialState extends State {

	/**
	 * default constructor
	 */
	public InitialState() {
		
	}
	
	@Override
	protected void eventEntry() {

	}

	@Override
	public <R> CompletableFuture<R> callBefore(TimeLimit limit, Callable<R> callable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isActive() {
		throw new UnsupportedOperationException();
	}
	
	

}
