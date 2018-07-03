package com.github.cjqcn.socks5.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author chenjinquan
 * @date 2018年6月21日21:34:00
 */
public class Socks5ClientBuilder {

    public static final int DEFUALT_PORT = 11081;
    public static final boolean DEFAULT_SHOULD_AUTH = false;

    public static Socks5Client create(int port, boolean shoudAuth) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("boss-thread"));
        EventLoopGroup workGroup = new NioEventLoopGroup(10, new DefaultThreadFactory("worker-thread"));
        return new Socks5Client().setPort(port).setBossGroup(bossGroup).setWorkGroup(workGroup).setShouldAuth
                (shoudAuth);
    }


    public static Socks5Client create() {
        return create(DEFUALT_PORT, DEFAULT_SHOULD_AUTH);
    }

    public static void main(String[] args) throws InterruptedException {
        Socks5ClientBuilder.create().start();
    }

}
