package com.github.cjqcn.socks5.client.handler;

import com.github.cjqcn.socks5.common.handler.Client2DestHandler;
import com.github.cjqcn.socks5.common.handler.Dest2ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

	private final static EventLoopGroup proxyGroup = new NioEventLoopGroup(20, new DefaultThreadFactory
			("proxy-thread"));

	private static final Logger LOG = LoggerFactory.getLogger(Socks5CommandRequestHandler.class);


	@Override
	protected void channelRead0(final ChannelHandlerContext clientChannelContext, final DefaultSocks5CommandRequest
			msg) throws Exception {

		if (msg.type().equals(Socks5CommandType.CONNECT)) {
			LOG.trace("准备连接目标服务器");

			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(proxyGroup)
					.channel(NioSocketChannel.class)
					.option(ChannelOption.TCP_NODELAY, true)
					.option(ChannelOption.SO_KEEPALIVE, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							//ch.pipeline().addLast(new IdleStateHandler(0, 0, 10,TimeUnit.SECONDS));
							ch.pipeline().addLast(new ReplySocks5CommandResponseHandler(clientChannelContext));
							ch.pipeline().addLast(new Dest2ClientHandler(clientChannelContext));
						}
					});
			ChannelFuture future = bootstrap.connect("127.0.0.1", 11080);
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(final ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						LOG.debug("成功连接目标服务器");
						clientChannelContext.pipeline().addLast(new Client2DestHandler(future));
						final ByteBuf buf = Unpooled.copiedBuffer(msg.dstAddr() + ":" + msg.dstPort() + "zjp",
								CharsetUtil.UTF_8);
						future.channel().writeAndFlush(buf);
					}
				}

			});
		} else {
			clientChannelContext.fireChannelRead(msg);
		}
	}

	class ReplySocks5CommandResponseHandler extends ChannelInboundHandlerAdapter {
		final ChannelHandlerContext clientChannelContext;
		private boolean inited = false;

		ReplySocks5CommandResponseHandler(ChannelHandlerContext clientChannelContext) {
			this.clientChannelContext = clientChannelContext;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (!inited) {
				inited = true;
				ByteBuf buf = (ByteBuf) msg;
				LOG.debug(String.valueOf((buf.toString(CharsetUtil.UTF_8))));
				Socks5CommandResponse commandResponse;
				if (buf.toString(CharsetUtil.UTF_8).equals("0000")) {
					commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus
							.SUCCESS, Socks5AddressType.IPv4);
				} else {
					commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus
							.FAILURE, Socks5AddressType.IPv4);
				}
				clientChannelContext.writeAndFlush(commandResponse);
			} else {
				ctx.fireChannelRead(msg);
			}
		}
	}
}
