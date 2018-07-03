package com.github.cjqcn.socks5.common.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jqChan
 * @date 2018/6/24
 */
public class Client2DestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(Client2DestHandler.class);

    private ChannelFuture destChannelFuture;

    public Client2DestHandler(ChannelFuture destChannelFuture) {
        this.destChannelFuture = destChannelFuture;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOG.trace("将客户端的消息转发给目标服务器端");
        destChannelFuture.channel().writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("客户端断开连接");
        super.channelInactive(ctx);
        destChannelFuture.channel().close();
    }
}