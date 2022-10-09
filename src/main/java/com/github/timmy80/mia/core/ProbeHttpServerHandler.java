package com.github.timmy80.mia.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * A Netty handler for the Probetask
 * @author anthony
 *
 */
public class ProbeHttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {

	private final ProbesTask probeTask;
	private FullHttpRequest fullRequest = null;
	
	/**
	 * Constructor
	 * @param probeTask request handling task
	 */
	public ProbeHttpServerHandler(ProbesTask probeTask) {
		this.probeTask = probeTask;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		if (msg instanceof HttpRequest) {
			HttpRequest request = (HttpRequest) msg;
			fullRequest = new DefaultFullHttpRequest(request.protocolVersion()
					, request.method()
					, request.uri()
					, ctx.alloc().buffer()
					, request.headers()
					, null);

			if (HttpUtil.is100ContinueExpected(request)) {
				writeResponse(ctx);
			}
		}

		if (msg instanceof HttpContent) {
			HttpContent httpContent = (HttpContent) msg;
			fullRequest.content().writeBytes(httpContent.content());

			if (msg instanceof LastHttpContent) {
				probeTask.runLater(probeTask::eventRequestReceived, ctx.channel(), fullRequest);
			}
		}
	}

	private void writeResponse(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
		ctx.write(response);
	}

}
