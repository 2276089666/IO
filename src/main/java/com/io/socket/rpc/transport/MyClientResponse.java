package com.io.socket.rpc.transport;

import com.io.socket.rpc.ResponseMappingCallback;
import com.io.socket.rpc.util.MsgPack;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author ws
 * @Date 2021/7/14 8:37
 */
public class MyClientResponse extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MsgPack msgPack = (MsgPack) msg;

        ResponseMappingCallback.runCallBack(msgPack);
    }
}
