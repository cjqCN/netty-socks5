package com.github.cjqcn.socks5.server.handler;

import com.github.cjqcn.socks5.common.handler.Client2DestHandler;
import com.github.cjqcn.socks5.common.handler.Dest2ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jqChan
 * @date 2018/6/24
 */
public class RequestInitHandler extends ChannelInboundHandlerAdapter {

    private final static EventLoopGroup proxyGroup = new NioEventLoopGroup(20, new DefaultThreadFactory("proxy-thread"));

    private static final Logger LOG = LoggerFactory.getLogger(RequestInitHandler.class);


    private boolean inited = false;

    @Override
    public void channelRead(final ChannelHandlerContext clientChannelContext, final Object msg) throws Exception {
        if (!inited) {
            ByteBuf buf = (ByteBuf) msg;
            String rawConnectMsg = buf.toString(CharsetUtil.UTF_8);
            String connectMsg = rawConnectMsg.substring(0, rawConnectMsg.indexOf("zjp"));
            String addr = connectMsg.substring(0, connectMsg.indexOf(":"));
            int port = Integer.valueOf(connectMsg.substring(connectMsg.indexOf(":") + 1, connectMsg.length()));
            LOG.debug("Server received:{}:{}", addr, port);
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
            ChannelFuture future = bootstrap.connect(addr, port);
            inited = true;
            future.addListener(new ChannelFutureListener() {
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        LOG.debug("成功连接目标服务器:{}:{}", addr, port);
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                        clientChannelContext.pipeline().addLast(new Client2DestHandler(future));
                        LOG.debug(String.valueOf(commandResponse));
                        clientChannelContext.writeAndFlush(commandResponse);
                    } else {
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
                        clientChannelContext.writeAndFlush(commandResponse);
                    }
                }

            });

        }
    }
}