package com.io.socket.selector.multipleSelector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Author ws
 * @Date 2021/7/8 18:13
 */
// selector
public class SelectorThread implements Runnable {

    LinkedBlockingDeque<Channel> queue = new LinkedBlockingDeque<>();
    Selector selector = null;
    // 利用SelectorThreadGroup的nextSelector()轮询的选择selector注册
    SelectorThreadGroup group = null;

    public SelectorThread(SelectorThreadGroup selectorThreadGroup) {
        try {
            this.group = selectorThreadGroup;
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                // select不带参数,每次调都会阻塞,上一次的注册,要等到下一次的wakeUp,才能被处理
                int num = selector.select();
                if (num > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        } else if (key.isWritable()) {

                        }
                    }
                }

                // 处理注册
                if (!queue.isEmpty()) {
                    Channel channel = queue.take();
                    // 服务端注册到selector
                    if (channel instanceof ServerSocketChannel) {

                        System.out.println(Thread.currentThread().getName() + " register listen...");
                        ServerSocketChannel server = (ServerSocketChannel) channel;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                        // 客户端注册到selector
                    } else if (channel instanceof SocketChannel) {

                        SocketChannel client = (SocketChannel) channel;
                        System.out.println(Thread.currentThread().getName() + " register client " + client.getRemoteAddress());
                        ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void readHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + " readHandler ....");
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        while (true) {
            try {
                int num = client.read(buffer);
                if (num > 0) {
                    // 翻转buffer,将其写出
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        System.out.println("write back ....");
                        // 在此将原内容写回
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (num == 0) {
                    break;
                } else {
                    // 客户端断开了
                    System.out.println("client " + client.getRemoteAddress() + "  close.......");
                    key.cancel();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + " acceptHandler ....");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);

            group.nextSelector(client);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
