package com.io.socket.aio;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;


/**
 * @Author ws
 * @Date 2021/7/17 12:58
 */
public class AioClient {
    @Test
    public void client() throws IOException {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
        // 连接到服务器并处理连接结果
        client.connect(new InetSocketAddress("localhost", 9090), null, new CompletionHandler<Void, Void>() {
            @Override
            public void completed( Void result,  Void attachment) {
                System.out.println("成功连接到服务器!");
                try {
                    // 给服务器发送信息并等待发送完成
                    client.write(ByteBuffer.wrap("From client:Hello i am client".getBytes()))
                            .get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(final Throwable exc, final Void attachment) {
                exc.printStackTrace();
            }
        });
    }
}
