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
 * JVM启动参数可以选择os的不同多路复用器的实现,没配置启动参数,JVM优先选择是epoll
 * -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
 * -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider
 */
public class SocketMultiplexingSingleThreadV1 {


    private ServerSocketChannel server = null;
    // 多路复用器
    private Selector selector = null;
    int port = 9090;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));


            // 根据jvm的启动参数,如果是epoll则会触发 epoll_create fd6
            selector = Selector.open();

            //server 约等于 listen状态的 fd4
            //register:
            //    如果选用os的select，poll：jvm里开辟一个数组 fd4 放进去
            //    如果选用os的epoll会触发epoll_ctl(fd6,ADD,fd4,EPOLLIN),但是是懒加载,等到select()调用时才触发
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

                Set<SelectionKey> keys = selector.keys();
                System.out.println("key size: " + keys.size());


                /*
                selector.select()无参的时候当有状态的fd集合为空的时候是阻塞的
                底层调用os的:
                    1，select，poll  其实是内核的select（fd4）  poll(fd4)
                    2，epoll：  其实是内核的 epoll_wait()
                selector.wakeup()  叫醒阻塞的select()
                 */

                // 阻塞的，当超时，或有注册的响应事件，或者被执行wakeup方法时继续
                selector.select(1000L);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();  //返回的有状态的fd集合
                Iterator<SelectionKey> iter = selectionKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove(); //set  不移除会重复循环处理
                    if (key.isAcceptable()) {
                        //看代码的时候，这里是重点，如果要去接受一个新的连接
                        //语义上，accept接受连接且返回新连接的FD对吧？
                        //那新的FD怎么办？
                        //select，poll，因为他们内核没有空间，那么在jvm中保存和前边的fd4那个listen的一起
                        //epoll： 我们希望通过epoll_ctl把新的客户端fd注册到内核空间的红黑树
                        acceptHandler(key);
                    } else if (key.isReadable()) {
                        //连read 还有 write都处理了
                        //在当前线程，这个方法可能会阻塞  ，如果阻塞了十年，其他的IO早就。。。
                        //所以，为什么提出了 IO THREADS
                        //redis  是不是用了epoll，redis是不是有个io threads的概念 ，redis是不是单线程的
                        //tomcat 8,9  异步的处理方式  IO  和   处理上  解耦
                        readHandler(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept(); //来啦，目的是调用accept接受客户端  fd4
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);

            //register:
            //    如果选用os的select，poll：jvm里开辟一个数组 fd5 放进去
            //    如果选用os的epoll会触发epoll_ctl(fd6,ADD,fd5,EPOLLIN)
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
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
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadV1 service = new SocketMultiplexingSingleThreadV1();
        service.start();
    }
}
