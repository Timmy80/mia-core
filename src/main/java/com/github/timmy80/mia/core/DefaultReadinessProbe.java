package com.github.timmy80.mia.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * The default readiness probe handler.<br>
 * Simply check that all {@link Task} are alive and not hung for more than 20seconds.<br>
 * Completes with status 200 OK if all the tasks are OK and the {@link ApplicationContext} is not stop pending. 
 * Completes with 503 SERVICE UNAVAILABLE otherwise.
 * @author anthony
 *
 */
public class DefaultReadinessProbe extends ProbeHandlerTerm {

	/**
	 * Constructor
	 * @param task the probes task (managed by {@link ApplicationContext}
	 * @param channel the channel with the probing peer
	 */
	public DefaultReadinessProbe(ProbesTask task, Channel channel) {
		super(task, channel);
	}

	@Override
	public void eventProbeCalled(HttpRequest request) {
		
		if(!task.getAppCtx().watchdogCheck(20000)) {
			complete(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE));
			return;
		}

		for( Task apptask : task.getAppCtx().taskSet()) {
			if(!apptask.isAlive()) {
				complete(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE));
				return;
			}
		}
		
		if(task.getAppCtx().isStopPending()) {
			complete(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE));
			return;
		}
		

		complete(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
	}

}
