package com.io.socket.netty.useAPI;

import com.io.socket.netty.ReadWriteHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.net.InetSocketAddress;

/**
 * @Author ws
 * @Date 2021/7/10 20:17
 */
public class NettyServer {

    @Test
    public void server() throws InterruptedException {
        ChannelFuture bind = new ServerBootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                // 不需要acceptHandler帮我们处理register了,框架帮我们干了
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new ReadWriteHandler());
                    }
                })
                .bind(new InetSocketAddress(9090));

        bind.sync().channel().closeFuture().sync();
        System.out.println("server close...");
    }
}