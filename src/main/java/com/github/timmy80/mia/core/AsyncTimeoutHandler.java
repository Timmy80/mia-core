package com.github.timmy80.mia.core;

import java.util.concurrent.Executor;

import com.github.timmy80.mia.core.Async.VoidFunction1;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

@Deprecated
public class AsyncTimeoutHandler implements TimerTask {
	
	protected final Executor executor;
	protected final VoidFunction1<Timeout> function;

	public AsyncTimeoutHandler(Task task, VoidFunction1<Timeout> function){
		this.executor = task;
		this.function = function;
	}

	@Override
	public void run(Timeout timeout) throws Exception {
		if(!timeout.isCancelled())
			Async.runLater(executor, function, timeout);
	}

}
