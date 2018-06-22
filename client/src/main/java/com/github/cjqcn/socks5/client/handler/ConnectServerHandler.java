package com.github.cjqcn.socks5.client.handler;


import com.github.cjqcn.socks5.client.Connect2Server;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectServerHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(ConnectServerHandler.class);
	private Connect2Server connect2Server = null;

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		connect2Server = Connect2Server.instance;
		super.channelActive(ctx);
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ctx.fireChannelRead(msg);
		connect2Server.writeAndFlush(msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		connect2Server = null;
		super.channelInactive(ctx);
	}
}
