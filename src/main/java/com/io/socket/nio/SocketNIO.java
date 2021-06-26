package com.io.socket.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * nio 同一个线程/进程同时处理多个连接，并读取数据
 */
public class SocketNIO {
    public static void main(String[] args) throws Exception {

        LinkedList<SocketChannel> clients = new LinkedList<>();

        ServerSocketChannel ss = ServerSocketChannel.open();  //服务端开启监听：接受客户端
        ss.bind(new InetSocketAddress(9090));
        ss.configureBlocking(false); //OS  NONBLOCKING!!!  保证监听的accept的系统调用是非阻塞的，没人连代码继续往下走


        while (true) {
            //接受客户端的连接
            Thread.sleep(1000);
            // 无论是否有连接，代码都会往下走，不会阻塞
            SocketChannel client = ss.accept(); // 系统调用accept,无连接不阻塞返回-1，java封装成无连接返回NULL


            if (client == null) {
                System.out.println("null.....");
            } else {
                client.configureBlocking(false); // 分配了fd的socket设为非阻塞的，保证接收读取文件的recv是非阻塞的
                int port = client.socket().getPort();
                System.out.println("client..port: " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);  //可以在堆里   堆外

            // 随着连接个数变多，这个集合也会变大，遍历起来也会变慢，连接的创建速度就会变慢
            //遍历已经链接进来的客户端能不能读写数据
            for (SocketChannel c : clients) {   //串行化！！！！  多线程！！
                int num = c.read(buffer);  // >0  -1  0   //不会阻塞
                if (num > 0) {
                    buffer.flip();
                    byte[] aaa = new byte[buffer.limit()];
                    buffer.get(aaa);

                    String b = new String(aaa);
                    System.out.println(c.socket().getPort() + " : " + b);
                    buffer.clear();
                }
            }
        }
    }

}
