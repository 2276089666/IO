package com.io.socket.rpc.transport;

import com.io.socket.rpc.Dispatcher;
import com.io.socket.rpc.protocol.MyContent;
import com.io.socket.rpc.protocol.MyHeader;
import com.io.socket.rpc.util.MsgPack;
import com.io.socket.rpc.util.ObjectToByteArray;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Author ws
 * @Date 2021/7/14 10:46
 */
public class ServerRequestHandler extends ChannelInboundHandlerAdapter {
    Dispatcher dis;

    public ServerRequestHandler(Dispatcher dis) {
        this.dis = dis;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        MsgPack requestPkg = (MsgPack) msg;
        String ioThread = Thread.currentThread().getName();

        // 使用netty的eventLoop的线程池帮我们处理业务read/write,也可以在当前线程处理业务

        /**
         * 1. ctx.executor().execute(new Runnable() {
         *
         * })
         *  会发现,执行i/o和service只有10个线程,因为10个client的socket连接,eventGroup的20个线程里面只有10个线程的selector被注册了,
         *  前面学的每个线程eventLoop处理i/o和service是线性的
         *  所以,每一个socket连接,在netty的模型下,处理i/o和service的是同一个线程
         */

        /**
         * 2.ctx.executor().parent().next().execute(new Runnable() {
         *
         * })
         *  处理i/o和service的不是同一个线程,对于service和i/o的所需要的处理时间不同,eventGroup线程池中的某些线程eventLoop可能会空闲,别的线程eventLoop会很忙,
         *  我们让空闲的线程eventLoop去帮忙执行别的selector的service业务处理
         */
        ctx.executor().parent().next().execute(new Runnable() {
            @Override
            public void run() {
                String serviceThread = Thread.currentThread().getName();
                System.out.println("ioThread: "+ioThread+"     serviceThread  "+serviceThread);
                String serviceName = requestPkg.getMyContent().getClassName();
                String method = requestPkg.getMyContent().getMethodName();
                Object c = dis.get(serviceName);
                Class<?> clazz = c.getClass();
                Object res = null;

                try {
                    Method m = clazz.getMethod(method, requestPkg.getMyContent().getParameterTypes());
                    res = m.invoke(c, requestPkg.getMyContent().getArgs());
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                // 构造返回的body
                MyContent content = new MyContent();
                content.setReturnData(res);
                byte[] contentByte = ObjectToByteArray.toByteArray(content);

                // 构造返回的header
                MyHeader resHeader = new MyHeader();
                resHeader.setRequestID(requestPkg.getMyHeader().getRequestID());
                resHeader.setFlag(0x24242424);
                resHeader.setDataLen(contentByte.length);

                byte[] headerByte = ObjectToByteArray.toByteArray(resHeader);

                ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(headerByte.length + contentByte.length);
                byteBuf.writeBytes(headerByte);
                byteBuf.writeBytes(contentByte);
                // 写回数据
                ctx.writeAndFlush(byteBuf);
            }
        });
    }
}
