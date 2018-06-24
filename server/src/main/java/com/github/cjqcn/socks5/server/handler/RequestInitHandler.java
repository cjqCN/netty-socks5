package com.github.cjqcn.socks5.server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jqChan
 * @date 2018/6/24
 */
public class RequestInitHandler extends ChannelInboundHandlerAdapter {

    private final static EventLoopGroup proxyGroup = new NioEventLoopGroup(2, new DefaultThreadFactory("proxy-thread"));

    private static final Logger LOG = LoggerFactory.getLogger(RequestInitHandler.class);


    private boolean inited = false;

    @Override
    public void channelRead(final ChannelHandlerContext clientChannelContext, final Object msg) throws Exception {
        if (!inited) {
            ByteBuf buf = (ByteBuf) msg;
            String rawConnectMsg = buf.toString(CharsetUtil.UTF_8);
            LOG.debug("Server received:{}", rawConnectMsg);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(proxyGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new Dest2ClientHandler(clientChannelContext));
                        }
                    });
            ChannelFuture future = bootstrap.connect("www.baidu.com", 443);
            inited = true;
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        LOG.debug("成功连接目标服务器:{}");
                        clientChannelContext.pipeline().addLast(new Client2DestHandler(future));
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                        clientChannelContext.writeAndFlush(commandResponse);
                    } else {
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
                        clientChannelContext.writeAndFlush(commandResponse);
                    }
                }

            });

        }
    }

    /**
     * 将目标服务器信息转发给客户端
     */
    private static class Dest2ClientHandler extends ChannelInboundHandlerAdapter {

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
            LOG.trace("目标服务器断开连接");
            super.channelInactive(ctx);
            clientChannelContext.channel().close();
        }
    }

    /**
     * 将客户端的消息转发给目标服务器端
     */
    private static class Client2DestHandler extends ChannelInboundHandlerAdapter {

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
            LOG.trace("客户端断开连接");
            super.channelInactive(ctx);
            destChannelFuture.channel().close();
        }
    }
}