package com.io.socket.selector.multipleSelectorV2;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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

    // 默认是boss,调用了addWorker会变成worker
    private SelectorThreadGroup workerGroup=this;

    public SelectorThreadGroup(int num,String threadName) {
        threads = new SelectorThread[num];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new SelectorThread(workerGroup);
            new Thread(threads[i],threadName+"thread id:\t"+i).start();
        }
    }

    public void addWorker(SelectorThreadGroup worker){
        workerGroup=worker;
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

        if (channel instanceof ServerSocketChannel){
            SelectorThread selectorThread = next();
            /**
             * 在bind的时候给serverGroup的每个线程的selector分配workerGroup
             */
            selectorThread.group=workerGroup;
            // 把注册的主动权交给各个SelectorThread 的run()方法里,用queue传递,代替上面的代码
            selectorThread.queue.add(channel);
            selectorThread.selector.wakeup();
        }else if (channel instanceof SocketChannel){
            SelectorThread selectorThread = next();
            // 把注册的主动权交给各个SelectorThread 的run()方法里,用queue传递,代替上面的代码
            selectorThread.queue.add(channel);
            selectorThread.selector.wakeup();
        }
    }

    private SelectorThread next() {
        int index = pos.incrementAndGet() % threads.length;
        return threads[index];
    }
}
