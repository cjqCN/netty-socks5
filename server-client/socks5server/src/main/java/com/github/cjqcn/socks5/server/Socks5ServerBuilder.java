package com.github.cjqcn.socks5.server;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author jqChan
 * @date 2018/6/13
 */
public class Socks5ServerBuilder {

    public static final int DEFUALT_PORT = 12080;
    public static final boolean DEFAULT_SHOULD_AUTH = false;

    public static Socks5Server create(int port, boolean shoudAuth) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("boss-thread"));
        EventLoopGroup workGroup = new NioEventLoopGroup(10, new DefaultThreadFactory("worker-thread"));
        return new Socks5Server().setPort(port).setBossGroup(bossGroup).setWorkGroup(workGroup).setShouldAuth
                (shoudAuth);
    }


    public static Socks5Server create() {
        return create(DEFUALT_PORT, DEFAULT_SHOULD_AUTH);
    }

    public static void main(String[] args) throws InterruptedException {
        Socks5ServerBuilder.create().start();
    }

}
