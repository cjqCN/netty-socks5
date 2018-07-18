package com.github.cjqcn.socks5.direct.server.handler;

import com.github.cjqcn.socks5.direct.server.socksproxy.DirectClientHandler;
import com.github.cjqcn.socks5.direct.server.socksproxy.RelayHandler;
import com.github.cjqcn.socks5.direct.server.socksproxy.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.socks.SocksAddressType.IPv4;

@ChannelHandler.Sharable
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

//	private final static EventLoopGroup proxyGroup = new NioEventLoopGroup(4, new DefaultThreadFactory
//			("proxy-thread"));

    private static final Logger LOG = LoggerFactory.getLogger(Socks5CommandRequestHandler.class);

//    @Override
//    protected void channelRead0(final ChannelHandlerContext clientChannelContext, final DefaultSocks5CommandRequest
//            msg) throws Exception {
//        LOG.debug("目标服务器  : " + msg.type() + "," + msg.dstAddr() + "," + msg.dstPort());
//        if (msg.type().equals(Socks5CommandType.CONNECT)) {
//            LOG.trace("准备连接目标服务器");
//            Bootstrap bootstrap = new Bootstrap();
//            bootstrap.group(clientChannelContext.channel().eventLoop())
//                    .channel(NioSocketChannel.class)
//                    .option(ChannelOption.TCP_NODELAY, true)
//                    .handler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        protected void initChannel(SocketChannel ch) throws Exception {
//                            //将目标服务器信息转发给客户端
//                            ch.pipeline().addLast(new Dest2ClientHandler(clientChannelContext));
//                        }
//                    });
//            LOG.trace("连接目标服务器");
//            ChannelFuture future = bootstrap.connect(msg.dstAddr(), msg.dstPort());
//            future.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(final ChannelFuture future) throws Exception {
//                    if (future.isSuccess()) {
//                        LOG.trace("成功连接目标服务器");
//                        clientChannelContext.pipeline().addLast(new Client2DestHandler(future));
//                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
//                        clientChannelContext.writeAndFlush(commandResponse);
//                    } else {
//                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
//                        clientChannelContext.writeAndFlush(commandResponse);
//                    }
//                }
//
//            });
//        } else {
//            clientChannelContext.fireChannelRead(msg);
//        }
//    }
    Bootstrap b = new Bootstrap();

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final DefaultSocks5CommandRequest request) throws Exception {
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(final Future<Channel> future) throws Exception {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, IPv4))
                                    .addListener(new ChannelFutureListener() {
                                        @Override
                                        public void operationComplete(ChannelFuture channelFuture) {
                                            ctx.pipeline().remove(Socks5CommandRequestHandler.this);
                                            outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()));
                                            ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                                        }
                                    });
                        } else {
                            ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, IPv4));
                            SocksServerUtils.closeOnFlush(ctx.channel());
                        }
                    }
                });

        final Channel inboundChannel = ctx.channel();
        b.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new DirectClientHandler(promise));

        b.connect(request.dstAddr(), request.dstPort()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // Connection established use handler provided results
                } else {
                    // Close the connection if the connection attempt has failed.
                    ctx.channel().writeAndFlush(
                            new SocksCmdResponse(SocksCmdStatus.FAILURE, IPv4));
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

}
