package com.github.timmy80.mia.core;

import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * A task that manages the probes configured on its ApplicationContext
 * @author anthony
 *
 */
public class ProbesTask extends Task {
	
	private static Logger logger = LogManager.getLogger(ProbesTask.class.getName());
	private final ProbesTask task = this;
	private ServerSocketChannel serverChannel;
	private CompletableFuture<Void> epilog;

	/**
	 * Constructor
	 * @param name task name
	 * @param appCtx parent {@link ApplicationContext}
	 * @throws IllegalArgumentException If parameters are set incorrectly, preventing execution
	 */
	public ProbesTask(String name, ApplicationContext appCtx) throws IllegalArgumentException {
		super(name, appCtx);
	}

	@Override
	public void eventStartTask() {
		try {
			serverChannel = openServerSocket(getAppCtx().getProbeInetHost(), getAppCtx().getProbeInetPort(), new ChannelInitializer<SocketChannel>() {
				
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
	                p.addLast(new HttpRequestDecoder());
	                p.addLast(new HttpResponseEncoder());
	                p.addLast(new ProbeHttpServerHandler(task));
				}
			});
		} catch (Exception e) {
			logger.fatal("Failed to open Probe server socket!", e);
			getAppCtx().stop();
		}
	}
	
	@Override
	protected void eventStopRequested() {
		listenFuture(serverChannel.close(), this::eventChannelClosed);
		registerEpilog(epilog);
	}
	
	/**
	 * Event callback method for closed server channel
	 * @param f the server socket channel future
	 */
	protected void eventChannelClosed(ChannelFuture f) {
		logger.info("Probe server channel closed!");
		epilog.complete(null);
	}
	
	/**
	 * event callback method for received request
	 * @param channel the requesting channel
	 * @param request the request
	 */
	protected void eventRequestReceived(Channel channel, FullHttpRequest request) {
		try {
			Class<ProbeHandlerTerm> handlerClass = getAppCtx().getProbes().get(request.uri());
			if(handlerClass == null) {
				channel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
					   .addListener(ChannelFutureListener.CLOSE);
				return ;
			}
			
			ProbeHandlerTerm handler  = handlerClass.getDeclaredConstructor(ProbesTask.class, Channel.class).newInstance(this, channel);
			handler.runLater(handler::eventProbeCalled, request);
		} catch (Exception e) {
			logger.error("Exception thrown while processing probe request.", e);
			channel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR))
			   .addListener(ChannelFutureListener.CLOSE);
		}
	}

}
