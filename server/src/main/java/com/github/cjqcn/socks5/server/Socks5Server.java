package com.github.cjqcn.socks5.server;


import com.github.cjqcn.socks5.common.Server;
import com.github.cjqcn.socks5.server.handler.AuditHandler;
import com.github.cjqcn.socks5.server.handler.ProxyIdleHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author jqChan
 * @date 2018/6/13
 */
public class Socks5Server implements Server {

    private static final Logger LOG = LoggerFactory.getLogger(Socks5Server.class);
    private Integer port;
    private Boolean shouldAuth;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;

    private ChannelGroup channelGroup;

    private State state;

    public Socks5Server() {
        state = State.ALREADY;
    }


    public synchronized void start() throws InterruptedException {
        if (state == State.ALREADY) {
            validate();
            try {
                LOG.info("Starting socks5Server, port is {}", port);
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
                            }
                        });
                ChannelFuture future = bootstrap.bind("0.0.0.0", port).sync();

                channelGroup.add(future.channel());

                LOG.info("Started socks5Server, port is {}", port);
                future.channel().closeFuture().sync();
            } finally {
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully();
                channelGroup.close().awaitUninterruptibly();

            }
        }
        if (state == State.RUNNING) {
            LOG.info("Ignore start() call on the socks5Server since it has already been started.");
            return;
        }
        if (state == State.STOPPED) {
            throw new IllegalStateException("Cannot start the socks5Server " +
                    "again since it has been stopped");
        }
        if (state == State.FAILED) {
            throw new IllegalStateException("Cannot start the socks5Server" +
                    " because it was failed earlier");
        }
    }

    public synchronized void stop() throws Exception {

        if (state == State.STOPPED) {
            LOG.debug("Ignore stop() call on socks5Server since it has already been stopped.");
            return;
        }
        LOG.info("Stopping socks5Server");
        try {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            channelGroup.close().awaitUninterruptibly();
        } catch (Exception ex) {
            state = State.FAILED;
            throw ex;
        }
        state = State.STOPPED;
        LOG.debug("Stopped socks5Server, port is {}", port);
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

    public Socks5Server setPort(int port) {
        this.port = port;
        return this;
    }

    public Socks5Server setShouldAuth(Boolean shouldAuth) {
        this.shouldAuth = shouldAuth;
        return this;
    }

    public Socks5Server setBossGroup(EventLoopGroup bossGroup) {
        this.bossGroup = bossGroup;
        return this;
    }

    public Socks5Server setWorkGroup(EventLoopGroup workGroup) {
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

