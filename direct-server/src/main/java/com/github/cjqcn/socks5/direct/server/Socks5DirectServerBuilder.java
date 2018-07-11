package com.github.cjqcn.socks5.direct.server;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author chenjinquan
 * @date 2018年6月21日21:34:00
 */
public class Socks5DirectServerBuilder {

    public static final int DEFUALT_PORT = 11080;
    public static final boolean DEFAULT_SHOULD_AUTH = false;

    public static Socks5DirectServer create(int port, boolean shoudAuth) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("boss-thread"));
        EventLoopGroup workGroup = new NioEventLoopGroup(10, new DefaultThreadFactory("worker-thread"));
        return new Socks5DirectServer().setPort(port).setBossGroup(bossGroup).setWorkGroup(workGroup).setShouldAuth
                (shoudAuth);
    }


    public static Socks5DirectServer create() {
        return create(DEFUALT_PORT, DEFAULT_SHOULD_AUTH);
    }

    public static void main(String[] args) throws InterruptedException {
        Socks5DirectServerBuilder.create().start();
    }

}
