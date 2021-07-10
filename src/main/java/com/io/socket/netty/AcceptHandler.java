package com.io.socket.netty;

import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;


/**
 * @Author ws
 * @Date 2021/7/10 16:52
 */
public class AcceptHandler extends ChannelInboundHandlerAdapter {
    private final ChannelHandler handler;
    private final EventLoopGroup selector;


    public AcceptHandler(ChannelHandler handler, EventLoopGroup selector) {
        this.handler = handler;
        this.selector = selector;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception{
        System.out.println("server  register...");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        System.out.println("server active...");
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        // 处理accept的handler,msg必是SocketChannel client
        NioSocketChannel client = (NioSocketChannel) msg;

        ChannelPipeline pipeline = client.pipeline();
        // 复用ReadWriteHandler来处理read/write
        pipeline.addLast(handler);

        selector.register(client);
    }
}
