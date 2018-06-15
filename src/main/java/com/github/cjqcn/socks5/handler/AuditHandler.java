package com.github.cjqcn.socks5.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditHandler extends ChannelInboundHandlerAdapter {


	private static final Logger LOG = LoggerFactory.getLogger(AuditHandler.class);

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		//LOG.debug(String.valueOf(msg));
		ctx.fireChannelRead(msg);
	}

}
