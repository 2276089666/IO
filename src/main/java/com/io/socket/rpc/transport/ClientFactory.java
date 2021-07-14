package com.io.socket.rpc.transport;

import com.io.socket.rpc.ResponseMappingCallback;
import com.io.socket.rpc.protocol.MyContent;
import com.io.socket.rpc.protocol.MyHeader;
import com.io.socket.rpc.util.ObjectToByteArray;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author ws
 * @Date 2021/7/13 16:47
 */
// 每个client可以有多个server,反之,每个server有多个client
// 单例
public class ClientFactory {
    // Key:一个远程server  value:多个client
    ConcurrentHashMap<InetSocketAddress, ClientPool> map = new ConcurrentHashMap<>();

    // 随机选择一个池中的client
    Random random=new Random();

    private ClientFactory() {
    }

    public static CompletableFuture transport(MyContent content) {
        byte[] body = ObjectToByteArray.toByteArray(content);
        MyHeader myHeader = MyHeader.createHeader(body);

        byte[] header = ObjectToByteArray.toByteArray(myHeader);
        System.out.println(Thread.currentThread().getName()+"msg header length:\t"+header.length);

        NioSocketChannel client = ClientFactory.getInstance().getClient(new InetSocketAddress("localhost", 9090));
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(header.length + body.length);

        long requestID = myHeader.getRequestID();

        CompletableFuture future= new CompletableFuture<>();
        ResponseMappingCallback.addCallBack(requestID,future);
        buffer.writeBytes(header);
        buffer.writeBytes(body);

        client.writeAndFlush(buffer);
        return future;
    }


    // 静态内部类,实现单例
    private static class FactoryHolder {
        private static final ClientFactory clientFactory = new ClientFactory();
    }

    public static ClientFactory getInstance() {
        return FactoryHolder.clientFactory;
    }


    public NioSocketChannel getClient(InetSocketAddress address) {
        ClientPool clientPool = map.get(address);
        if (clientPool==null){
            map.putIfAbsent(address,new ClientPool(10));
            clientPool=map.get(address);
        }
        int maxClientSize = clientPool.getMaxClientSize();
        int index = random.nextInt(maxClientSize);

        if (clientPool.clients[index]!=null&&clientPool.clients[index].isActive()){
            return clientPool.clients[index];
        }else {
            synchronized (clientPool.locks[index]){
                if (clientPool.clients[index]==null||!clientPool.clients[index].isActive()){
                    clientPool.clients[index]=createClient(address);
                    return clientPool.clients[index];
                }
            }
        }
        return clientPool.clients[index];
    }

    // 基于netty的client创建方式
    private NioSocketChannel createClient(InetSocketAddress address) {
        NioEventLoopGroup group = new NioEventLoopGroup(2);
        ChannelFuture connect = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        // 对ChannelPipeline堆积的bytebuf解码,解决粘包问题
                        pipeline.addLast(new MyDecode());
                        pipeline.addLast(new MyClientResponse());
                    }
                })
                .connect(address);
        try {
            NioSocketChannel client = (NioSocketChannel) connect.sync().channel();
            return client;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
