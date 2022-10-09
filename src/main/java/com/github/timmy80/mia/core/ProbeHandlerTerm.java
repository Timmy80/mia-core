package com.github.timmy80.mia.core;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Timeout;

/**
 * Base class for a liveness, readiness, ... probe request handling
 * @author anthony
 *
 */
public abstract class ProbeHandlerTerm extends Terminal<ProbesTask> {

	private static Logger logger = LogManager.getLogger(ProbeHandlerTerm.class.getName());
	
	/**
	 * The request completion future
	 */
	protected final CompletableFuture<HttpResponse> completionFuture;
	/**
	 * The requesting channel
	 */
	protected final Channel channel;
	/**
	 * The response timeout
	 */
	protected final Timeout timeout;
	
	/**
	 * Constructor
	 * @param task the probes task (managed by {@link ApplicationContext}
	 * @param channel the channel with the probing peer
	 */
	public ProbeHandlerTerm(ProbesTask task, Channel channel) {
		super(task);
		this.completionFuture = new CompletableFuture<>();
		this.channel = channel;
		this.timeout = this.newTimeout(5000, this::eventResponseTimeout);
		listenFuture(this.completionFuture, this::eventResponseComplete);
	}
	
	/**
	 * Complete this probing request
	 * @param response an instance of {@link HttpResponse}
	 */
	public void complete(HttpResponse response) {
		completionFuture.complete(response);
	}
	
	private void eventResponseTimeout(Timeout t) {
		completionFuture.complete(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.GATEWAY_TIMEOUT));
	}

	private void eventResponseComplete(CompletableFuture<HttpResponse> f) {
		try {
			timeout.cancel();
			channel.writeAndFlush(f.get()).addListener(ChannelFutureListener.CLOSE);
		} catch (Exception e) {
			logger.error("Exception while processing probe response.", e);
			channel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
				   .addListener(ChannelFutureListener.CLOSE);
		} finally {
			terminate();
		}
	}
	
	/**
	 * Method implemented by sub classes to handle the probing request
	 * @param request the probing http request
	 */
	public abstract void eventProbeCalled(HttpRequest request);
	
}
