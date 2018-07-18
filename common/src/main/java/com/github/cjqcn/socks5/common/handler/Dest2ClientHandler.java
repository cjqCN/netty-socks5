package com.github.cjqcn.socks5.common.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
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
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx2, Object destMsg) throws Exception {
        LOG.trace("将目标服务器信息转发给客户端");
        if (clientChannelContext.channel().isActive()) {
            clientChannelContext.channel().writeAndFlush(destMsg);
        } else {
            ReferenceCountUtil.release(destMsg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("目标服务器断开连接");
        if (clientChannelContext.channel().isActive()) {
            clientChannelContext.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}