package com.github.cjqcn.socks5.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public class Connect2Server extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(Connect2Server.class);

	private final static EventLoopGroup connectGroup = new NioEventLoopGroup(1, new DefaultThreadFactory
			("connect-thread"));

	private boolean isConnected = false;

	private LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue<Object>(100000);

	private Channel channel = null;

	public static Connect2Server instance = new Connect2Server();

	private Connect2Server() {
		super("Connect2Server");
		connection();
		this.start();

	}

	private synchronized void connection() {
		LOG.debug("准备连接代理服务器");
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(connectGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new IdleStateHandler(3, 30, 0));
					}
				});
		ChannelFuture future = bootstrap.connect("127.0.0.1", 11080);
		future.addListener(new ChannelFutureListener() {
			public void operationComplete(final ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					LOG.debug("成功连接代理服务器");
					isConnected = true;
					channel = future.channel();
				}
			}
		});
	}

	public void writeAndFlush(Object o) throws InterruptedException {
		linkedBlockingQueue.put(o);
	}

	@Override
	public void run() {
		while (true) {
			Object o = null;
			try {
				o = linkedBlockingQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (o != null) {
				channel.writeAndFlush(o);
			}
		}
	}

	/**
	 * 将客户端的消息转发给目标服务器端
	 */
	private static class TranspondHandler extends ChannelInboundHandlerAdapter {

		private static final Logger LOG = LoggerFactory.getLogger(TranspondHandler.class);

		private ChannelFuture channelFuture;

		public TranspondHandler(ChannelFuture destChannelFuture) {
			this.channelFuture = destChannelFuture;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			LOG.debug("将客户端的消息转发给代理服务器端");
			channelFuture.channel().writeAndFlush(msg);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			channelFuture.channel().close();
			LOG.info("断开到连接");
		}
	}
}
