package com.github.cjqcn.socks5.direct.server.handler;

import com.github.cjqcn.socks5.common.handler.Client2DestHandler;
import com.github.cjqcn.socks5.common.handler.Dest2ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

	private final static EventLoopGroup proxyGroup = new NioEventLoopGroup(4, new DefaultThreadFactory
			("proxy-thread"));

	private static final Logger LOG = LoggerFactory.getLogger(Socks5CommandRequestHandler.class);

	@Override
	protected void channelRead0(final ChannelHandlerContext clientChannelContext, final DefaultSocks5CommandRequest
			msg) throws Exception {
		LOG.debug("目标服务器  : " + msg.type() + "," + msg.dstAddr() + "," + msg.dstPort());
		if(msg.type().equals(Socks5CommandType.CONNECT)) {
			LOG.trace("准备连接目标服务器");
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(proxyGroup)
					.channel(NioSocketChannel.class)
					.option(ChannelOption.TCP_NODELAY, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							//将目标服务器信息转发给客户端
							ch.pipeline().addLast(new Dest2ClientHandler(clientChannelContext));
						}
					});
			LOG.trace("连接目标服务器");
			ChannelFuture future = bootstrap.connect(msg.dstAddr(), msg.dstPort());
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(final ChannelFuture future) throws Exception {
					if(future.isSuccess()) {
						LOG.trace("成功连接目标服务器");
						clientChannelContext.pipeline().addLast(new Client2DestHandler(future));
						Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
						clientChannelContext.writeAndFlush(commandResponse);
					} else {
						Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
						clientChannelContext.writeAndFlush(commandResponse);
					}
				}

			});
		} else {
			clientChannelContext.fireChannelRead(msg);
		}
	}

}
