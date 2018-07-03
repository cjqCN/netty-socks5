package com.github.cjqcn.socks5.common.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jqChan
 * @date 2018/6/24
 */
public class Dest2ClientHandler extends ChannelInboundHandlerAdapter {

    private ChannelHandlerContext clientChannelContext;

    private static final Logger LOG = LoggerFactory.getLogger(Dest2ClientHandler.class);

    public Dest2ClientHandler(ChannelHandlerContext clientChannelContext) {
        this.clientChannelContext = clientChannelContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx2, Object destMsg) throws Exception {
        LOG.trace("将目标服务器信息转发给客户端");
        clientChannelContext.writeAndFlush(destMsg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("目标服务器断开连接");
        super.channelInactive(ctx);
        clientChannelContext.channel().close();
    }
}