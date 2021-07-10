package com.io.socket.netty;

import io.netty.channel.*;

/**
 * @Author ws
 * @Date 2021/7/10 19:18
 */
@ChannelHandler.Sharable
public class MyChannelInitializer extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new ReadWriteHandler());
        // ChannelInitializer这个handler就是为了解ReadWriteHandler得耦,没有任何作用,所以可以在pipeline将其移除
        ctx.pipeline().remove(this);
    }
}
