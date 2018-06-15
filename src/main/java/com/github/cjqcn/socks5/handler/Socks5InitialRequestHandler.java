package com.github.cjqcn.socks5.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

	private static final Logger LOG = LoggerFactory.getLogger(Socks5InitialRequestHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
		LOG.debug(String.valueOf(msg));
		ctx.fireChannelRead(msg);
		Socks5InitialResponse initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
		ctx.writeAndFlush(initialResponse);

	}
}
