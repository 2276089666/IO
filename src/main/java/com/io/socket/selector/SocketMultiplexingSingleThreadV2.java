package com.io.socket.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多线程版本
 */
public class SocketMultiplexingSingleThreadV2 {

    private ServerSocketChannel server = null;
    private Selector selector = null;   //linux 多路复用器（select poll epoll） nginx  event{}
    int port = 9090;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            selector = Selector.open();  //  select  poll  *epoll
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了。。。。。");
        try {
            while (true) {
                // 阻塞的，当超时，或有注册的响应事件，或者被执行wakeup方法时继续
                selector.select(1000L);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectionKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) {
                        acceptHandler(key);
                    } else if (key.isReadable()) {
                        // 避免readHandler里面的线程还没起来,register新的状态
                        // 但是由于非阻塞,第二次循环又来了导致重复消费
                        key.cancel();
                        readHandler(key);//还是阻塞的嘛？ 即便以抛出了线程去读取，但是在时差里，这个key的read事件会被重复触发

                    } else if (key.isWritable()) {
                        // 避免readHandler里面的线程还没起来,register新的状态
                        // 但是由于非阻塞,第二次循环又来了导致重复消费
                        key.cancel();
                        writeHandler(key);
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHandler(SelectionKey key) {
        new Thread(() -> {
            System.out.println("write handler...");
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.flip();
            while (buffer.hasRemaining()) {
                try {
                    client.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            buffer.clear();
            try {
                client.shutdownOutput();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "Write Thread " + Thread.currentThread().getId()).start();

    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        new Thread(() -> {
            System.out.println("read handler.....");
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();
            int read = 0;
            try {
                while (true) {
                    read = client.read(buffer);
                    System.out.println(Thread.currentThread().getName() + " " + read);
                    if (read > 0) {
                        // 让读到得重新写给client
                        client.register(key.selector(), SelectionKey.OP_WRITE, buffer);
                    } else if (read == 0) {
                        break;
                    } else {
                        client.close();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "Read Thread " + Thread.currentThread().getId()).start();

    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadV2 service = new SocketMultiplexingSingleThreadV2();
        service.start();
    }
}
