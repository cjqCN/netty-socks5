package com.github.cjqcn.socks5.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditHandler extends ChannelInboundHandlerAdapter {


	private static final Logger LOG = LoggerFactory.getLogger(AuditHandler.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		LOG.info("接收到连接");
		LOG.info("初始化AuditHandler");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		LOG.debug(String.valueOf(msg));
		ctx.fireChannelRead(msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		LOG.info("断开到连接");
	}
}
