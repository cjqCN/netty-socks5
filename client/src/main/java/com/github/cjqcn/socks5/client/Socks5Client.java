package com.github.cjqcn.socks5.client;

import com.github.cjqcn.socks5.client.handler.*;
import com.github.cjqcn.socks5.common.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks5Client implements Server {

	private static final Logger LOG = LoggerFactory.getLogger(Socks5Client.class);
	private Integer port;
	private Boolean shouldAuth;

	private EventLoopGroup bossGroup;
	private EventLoopGroup workGroup;

	private ChannelGroup channelGroup;

	private State state;

	public Socks5Client() {
		state = State.ALREADY;
	}


	public synchronized void start() throws InterruptedException {
		if (state == State.ALREADY) {
			validate();
			try {
				LOG.info("Starting socks5Client, port is {}", port);
				channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
				ServerBootstrap bootstrap = new ServerBootstrap();
				bootstrap.group(bossGroup, workGroup)
						.channel(NioServerSocketChannel.class)
						.option(ChannelOption.SO_BACKLOG, 1024)
						.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
						.childHandler(new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(SocketChannel ch) throws Exception {
								channelGroup.add(ch);
								ChannelPipeline channelPipeline = ch.pipeline();
								//channel超时处理
								ch.pipeline().addLast(new IdleStateHandler(3, 30, 0));
								ch.pipeline().addLast(new ProxyIdleHandler());

								//审计日志
								channelPipeline.addLast("AuditHandler", new AuditHandler());

								//socks5ClientEncoder
								ch.pipeline().addLast("socks5ClientEncoder",
										Socks5ServerEncoder.DEFAULT);

								//初始化解码器
								channelPipeline.addLast("Socks5InitialRequestDecoder",
										new Socks5InitialRequestDecoder());
								//初始化处理器
								channelPipeline.addLast("Socks5InitialRequestHandler",
										new Socks5InitialRequestHandler());

								//socks请求解码器
								channelPipeline.addLast("Socks5CommandRequestDecoder",
										new Socks5CommandRequestDecoder());
								//socks处理器
								channelPipeline.addLast("Socks5CommandRequestHandler",
										new Socks5CommandRequestHandler());

								channelPipeline.addLast("ConnectServerHandler",
										new ConnectServerHandler());
							}
						});
				ChannelFuture future = bootstrap.bind("0.0.0.0", port).sync();

				channelGroup.add(future.channel());

				LOG.info("Started socks5Client, port is {}", port);
				future.channel().closeFuture().sync();
			} finally {
				bossGroup.shutdownGracefully();
				workGroup.shutdownGracefully();
				channelGroup.close().awaitUninterruptibly();

			}
		}
		if (state == State.RUNNING) {
			LOG.info("Ignore start() call on the socks5Client since it has already been started.");
			return;
		}
		if (state == State.STOPPED) {
			throw new IllegalStateException("Cannot start the socks5Client " +
					"again since it has been stopped");
		}
		if (state == State.FAILED) {
			throw new IllegalStateException("Cannot start the socks5Client" +
					" because it was failed earlier");
		}
	}

	public synchronized void stop() throws Exception {

		if (state == State.STOPPED) {
			LOG.debug("Ignore stop() call on socks5Client since it has already been stopped.");
			return;
		}
		LOG.info("Stopping socks5Client");
		try {
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
			channelGroup.close().awaitUninterruptibly();
		} catch (Exception ex) {
			state = State.FAILED;
			throw ex;
		}
		state = State.STOPPED;
		LOG.debug("Stopped socks5Client, port is {}", port);
	}


	private void validate() {
		if (port == null) {
			throw new RuntimeException("Unset port");
		}
		if (shouldAuth == null) {
			throw new RuntimeException("Unset shouldAuth");
		}
		if (bossGroup == null) {
			throw new RuntimeException("Unset bossGroup");
		}
		if (workGroup == null) {
			throw new RuntimeException("Unset workGroup");
		}
	}

	public Socks5Client setPort(int port) {
		this.port = port;
		return this;
	}

	public Socks5Client setShouldAuth(Boolean shouldAuth) {
		this.shouldAuth = shouldAuth;
		return this;
	}

	public Socks5Client setBossGroup(EventLoopGroup bossGroup) {
		this.bossGroup = bossGroup;
		return this;
	}

	public Socks5Client setWorkGroup(EventLoopGroup workGroup) {
		this.workGroup = workGroup;
		return this;
	}

	public enum State {
		ALREADY,
		RUNNING,
		STOPPED,
		FAILED
	}
}
