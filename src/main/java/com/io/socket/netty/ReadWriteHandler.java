package com.io.socket.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * @Author ws
 * @Date 2021/7/10 16:44
 */

/**
 *  多个client连接过来,每个client都得有对应得ReadWriteHandler
 *  @ChannelHandler.Sharable
 *  让单例的ReadWriteHandler可以被多个client共享,但是不能有成员变量
 *  ReadWriteHandler处理业务逻辑的不能有成员变量是不行的
 *  所以,netty使用ChannelInitializer抽象类来帮我们解决这个问题,让ChannelInitializer可共享
 *  ChannelInitializer类可以没有成员变量,我们需要实现它的initChannel方法,把我们的handler 传入到ChannelPipeline
 *  ChannelPipeline pipeline = client.pipeline();
 *  pipeline.addLast(handler);
 *  在此,我使用MyChannelInitializer模仿netty
 */
//@ChannelHandler.Sharable
public class ReadWriteHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception{
        System.out.println("client  register...");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        System.out.println("client active...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        // 处理read/write的handler,msg必是ByteBuf
        ByteBuf byteBuf = (ByteBuf) msg;
        // readCharSequence会移动指针,pos,导致我们的writeAndFlush写出去的是空白
//        CharSequence sequence = byteBuf.readCharSequence(byteBuf.readableBytes(), CharsetUtil.UTF_8);
        CharSequence sequence = byteBuf.getCharSequence(0, byteBuf.readableBytes(), CharsetUtil.UTF_8);
        System.out.println(sequence.toString());

        // 重新写回去
        ctx.writeAndFlush(byteBuf);
    }
}
