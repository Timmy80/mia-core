package com.github.timmy80.mia.core;

import io.netty.util.Timeout;

/**
 * Extend {@linkplain io.netty.util.TimerTask} to make it a FunctionalInterface
 * @author anthony
 *
 */
@FunctionalInterface
public interface TimerTask extends io.netty.util.TimerTask {

	@Override
	public void run(Timeout timeout);
}
