package com.io.socket.rpc.transport;


import com.io.socket.rpc.protocol.MyContent;
import com.io.socket.rpc.protocol.MyHeader;
import com.io.socket.rpc.util.Config;
import com.io.socket.rpc.util.MsgPack;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * @Author ws
 * @Date 2021/7/14 8:37
 */
// 序列化的方式解码
// 把ByteBuf变成我们的对象MsgPack
public class MyDecode extends ByteToMessageDecoder {
    // ByteToMessageDecoder.channelRead()会把上一次bytebuf装不下,而截断的对象,合并到下一次的bytebuf中,再次序列化(重写的这个decode()方法)
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        // 当多个线程复用一个连接时,即ClientPool只有一个连接时,会出现粘包的情况,多个 header body header body header body连在了一起
        while (byteBuf.readableBytes()>= Config.headerSize){
            byte[] header = new byte[Config.headerSize];
            // 从哪里读,读多少,但是readIndex不变
            // getBytes不移动指针,避免bytebufer里面有截断的对象,我们把截断的对象留到下一次read,保证对象能被序列化
            byteBuf.getBytes(byteBuf.readerIndex(),header);
            ByteArrayInputStream ain = new ByteArrayInputStream(header);
            ObjectInputStream oin = new ObjectInputStream(ain);
            MyHeader myHeader = (MyHeader) oin.readObject();

            System.out.println("RequestID:  "+myHeader.getRequestID());

            if (byteBuf.readableBytes()>=myHeader.getDataLen()){
                // 移动指针,到body位置
                byteBuf.readBytes(Config.headerSize);
                byte[] body = new byte[(int) myHeader.getDataLen()];
                byteBuf.readBytes(body);
                ByteArrayInputStream dain = new ByteArrayInputStream(body);
                ObjectInputStream doin = new ObjectInputStream(dain);
                // client的包的协议解码
                if (myHeader.getFlag()==0x14141414){
                    MyContent myContent = (MyContent) doin.readObject();
                    list.add(new MsgPack(myHeader,myContent));
                }else if (myHeader.getFlag()==0x24242424){
                    // server的包的协议解码
                    MyContent myContent = (MyContent) doin.readObject();
                    list.add(new MsgPack(myHeader,myContent));
                }
            }else {
                // 当前的bytebuf的剩余数据的body不够,直接跳出.和下个bytebuf去merge交给下一次的bytebuf的decode解决
                break;
            }
        }
    }
}
