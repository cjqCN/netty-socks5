package com.github.cjqcn.socks5.server.handler;

import com.github.cjqcn.socks5.common.handler.Client2DestHandler;
import com.github.cjqcn.socks5.common.handler.Dest2ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jqChan
 * @date 2018/6/24
 */
public class RequestInitHandler extends ChannelInboundHandlerAdapter {

    private final static EventLoopGroup proxyGroup = new NioEventLoopGroup(20, new DefaultThreadFactory
            ("proxy-thread"));

    private static final Logger LOG = LoggerFactory.getLogger(RequestInitHandler.class);


    private boolean inited = false;

    @Override
    public void channelRead(final ChannelHandlerContext clientChannelContext, final Object msg) throws Exception {
        if (!inited) {
            inited = true;
            ByteBuf buf = (ByteBuf) msg;
            String rawConnectMsg = buf.toString(CharsetUtil.UTF_8);
            buf.release();
            String connectMsg = rawConnectMsg.substring(0, rawConnectMsg.indexOf("zjp"));
            final String addr = connectMsg.substring(0, connectMsg.indexOf(":"));
            final int port = Integer.valueOf(connectMsg.substring(connectMsg.indexOf(":") + 1, connectMsg.length()));
            LOG.debug("Server received:{}:{}", addr, port);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(proxyGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new Dest2ClientHandler(clientChannelContext));
                        }
                    });
            ChannelFuture future = bootstrap.connect(addr, port);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        LOG.debug("成功连接目标服务器:{}:{}", addr, port);
                        clientChannelContext.pipeline().addLast(new Client2DestHandler(future));
                        final ByteBuf buf = Unpooled.copiedBuffer("0000",
                                CharsetUtil.UTF_8);
                        clientChannelContext.writeAndFlush(buf);
                    } else {
                        final ByteBuf buf = Unpooled.copiedBuffer("1111",
                                CharsetUtil.UTF_8);
                        clientChannelContext.writeAndFlush(buf);
                    }
                }
            });
        } else {
            clientChannelContext.fireChannelRead(msg);
        }
    }
}