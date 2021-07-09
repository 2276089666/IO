package com.io.socket.selector.multipleSelector;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author ws
 * @Date 2021/7/8 18:13
 */
public class SelectorThreadGroup {

    // 多个selector
    private SelectorThread[] threads = null;

    private ServerSocketChannel server = null;

    private AtomicInteger pos = new AtomicInteger(0);

    public SelectorThreadGroup(int num) {
        threads = new SelectorThread[num];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new SelectorThread(this);
            new Thread(threads[i]).start();
        }
    }


    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            // 注册到某个线程的selector
            nextSelector(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void nextSelector(Channel channel) {
        SelectorThread selectorThread = next();
//        if (channel instanceof ServerSocketChannel) {
//            try {
//                ServerSocketChannel sc = (ServerSocketChannel) channel;
////                selectorThread.selector.wakeup();
//                sc.register(selectorThread.selector, SelectionKey.OP_ACCEPT);
//                // 具体的SelectorThread会在selector.select()阻塞,register注册不进去,register会阻塞
//                selectorThread.selector.wakeup();
//            } catch (ClosedChannelException e) {
//                e.printStackTrace();
//            }
//        }
        // 把注册的主动权交给各个SelectorThread 的run()方法里,用queue传递,代替上面的代码
        selectorThread.queue.add(channel);
        selectorThread.selector.wakeup();

    }

    private SelectorThread next() {
        int index = pos.incrementAndGet() % threads.length;
        return threads[index];
    }
}
