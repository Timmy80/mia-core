package com.github.timmy80.mia.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * The default liveness probe handler.<br>
 * Simply check that no {@link Task} is hung for more than 20seconds.<br>
 * Completes with status 200 OK if all the tasks are OK. Completes with 500 INTERNAL ERROR otherwise.
 * @author anthony
 *
 */
public class DefaultLivenessProbe extends ProbeHandlerTerm {

	/**
	 * Constructor
	 * @param task the probes task (managed by {@link ApplicationContext}
	 * @param channel the channel with the probing peer
	 */
	public DefaultLivenessProbe(ProbesTask task, Channel channel) {
		super(task, channel);
	}

	@Override
	public void eventProbeCalled(HttpRequest request) {
		if(task.getAppCtx().watchdogCheck(20000))
			complete(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
		else
			complete(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
		
	}

}
