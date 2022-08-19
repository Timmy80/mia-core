package com.github.timmy80.mia.core;

/**
 * Parameters of an {@link ApplicationContext}
 * @author anthony
 *
 */
public class ApplicationContextParams {

	/**
	 * Number of Threads allocated to the networking stack.
	 */
	private Integer netThreads = null;
	
	public Integer getNetThreads() {
		return netThreads;
	}

	public void setNetThreads(Integer netThreads) {
		
		this.netThreads = netThreads;
	}
	
}
