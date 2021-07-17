package com.io.socket.netty.client;

import com.io.socket.netty.MyChannelInitializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.net.InetSocketAddress;


/**
 * @Author ws
 * @Date 2021/7/10 11:12
 */
public class TestNettyClient {
    @Test
    public void client() throws InterruptedException {
        NioSocketChannel client = new NioSocketChannel();

        // pipeline,给出read/write的处理逻辑
        ChannelPipeline pipeline = client.pipeline();

        // ChannelInitializer对ReadWriteHandler无法@ChannelHandler.Sharable解耦
        // pipeline.addLast(new  ReadWriteHandler());
        pipeline.addLast(new MyChannelInitializer());

        // 相当于一个线程池,里面初始化了一个线程,就是我们的selector
        NioEventLoopGroup selector = new NioEventLoopGroup(1);
        // 注册和之前的JDK的方式不一样,之前是client.register(selector,SelectionKey.OP_ACCEPT)
        selector.register(client);


        // 必须先register再连接,并且连接和发送都是异步的,由于得先连接再发送,所以得sync
        ChannelFuture connect = client.connect(new InetSocketAddress("172.16.136.145", 9090));
        connect.sync();


        // write
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server ~".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf);
        send.sync();


        // 用异步感知,是否断开连接
        connect.channel().closeFuture().sync();
        selector.shutdownGracefully();
        System.out.println("over ...");
    }
}

