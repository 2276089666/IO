package com.io.socket.netty.useAPI;

import com.io.socket.netty.ReadWriteHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.net.InetSocketAddress;

/**
 * @Author ws
 * @Date 2021/7/10 20:17
 */
public class NettyClient {

    @Test
    public void client() throws InterruptedException {
        ChannelFuture connect = new Bootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioSocketChannel.class)
                // 和我的MyChannelInitializer作用类似
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new ReadWriteHandler());
                    }
                })
                .connect(new InetSocketAddress("172.16.136.145", 9090));

        Channel client = connect.sync().channel();

        // write
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server ~".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf);
        send.sync();


        // 用异步感知,是否断开连接
        connect.channel().closeFuture().sync();
        System.out.println("over ...");
    }
}
