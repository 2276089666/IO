package com.io.socket.netty.server;

import com.io.socket.netty.AcceptHandler;
import com.io.socket.netty.ReadWriteHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.Test;

import java.net.InetSocketAddress;

/**
 * @Author ws
 * @Date 2021/7/10 16:43
 */
public class TestNettyServer {
    @Test
    public void Server() throws InterruptedException {
        NioServerSocketChannel server = new NioServerSocketChannel();

        NioEventLoopGroup selector = new NioEventLoopGroup(1);

        ChannelPipeline pipeline = server.pipeline();
        // AcceptHandler accept接收客户端，并且把client注册到selector,两件事
        // 复用ReadWriteHandler去处理read/write
        pipeline.addLast(new AcceptHandler(new ReadWriteHandler(),selector));

        selector.register(server);

        ChannelFuture bind = server.bind(new InetSocketAddress(9090));
        bind.sync();

        bind.sync().channel().closeFuture().sync();
        selector.shutdownGracefully();
        System.out.println("server close...");
    }
}
