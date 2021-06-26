package com.io.socket.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO  多线程的方式
 */
public class SocketIOPropertites {


    //server socket listen property:
    private static final int RECEIVE_BUFFER = 10;
    private static final int SO_TIMEOUT = 0;
    private static final boolean REUSE_ADDR = false;
    private static final int BACK_LOG = 2;          // 创建连接后备socket队列的大小，内核最多可以有1+2个socket的四元组建立
    //client socket listen property on server endpoint:
    private static final boolean CLI_KEEPALIVE = false; // 会发包试探客户端是否还活着，客户端会回应，可以把一些死连接在内核占用的资源释放掉
    private static final boolean CLI_OOB = false;
    private static final int CLI_REC_BUF = 20;
    private static final boolean CLI_REUSE_ADDR = false;
    private static final int CLI_SEND_BUF = 20;
    private static final boolean CLI_LINGER = true;
    private static final int CLI_LINGER_N = 0;
    private static final int CLI_TIMEOUT = 0;
    private static final boolean CLI_NO_DELAY = false;  // 开启优化，在优化开启时会积攒超过sendBuffer大小的数据一起发送，某些情况下提高效率
/*

    StandardSocketOptions.TCP_NODELAY
    StandardSocketOptions.SO_KEEPALIVE
    StandardSocketOptions.SO_LINGER
    StandardSocketOptions.SO_RCVBUF
    StandardSocketOptions.SO_SNDBUF
    StandardSocketOptions.SO_REUSEADDR

 */


    public static void main(String[] args) {

        ServerSocket server = null;
        try {
            // 创建连接的socket
            server = new ServerSocket();
            server.bind(new InetSocketAddress(9090), BACK_LOG);
            server.setReceiveBufferSize(RECEIVE_BUFFER);
            server.setReuseAddress(REUSE_ADDR);
            server.setSoTimeout(SO_TIMEOUT);


            System.out.println("server up use 9090!");

            while (true) {

                //分水岭,会发现socket建立在内核了，三次握手也ok了，buffer也分配了，等着server.accept()让内核分配文件标识符FD,然后app通过FD去对应的socket的buffer取数据
                System.in.read();

                // 传输数据的socket
                Socket client = server.accept();  //阻塞的，没有 -1  一直卡着不动  accept(4,
                System.out.println("client port: " + client.getPort());

                client.setKeepAlive(CLI_KEEPALIVE);
                client.setOOBInline(CLI_OOB);
                client.setReceiveBufferSize(CLI_REC_BUF);
                client.setReuseAddress(CLI_REUSE_ADDR);
                client.setSendBufferSize(CLI_SEND_BUF);
                client.setSoLinger(CLI_LINGER, CLI_LINGER_N);
                client.setSoTimeout(CLI_TIMEOUT);
                client.setTcpNoDelay(CLI_NO_DELAY);

                //client.read   //阻塞   没有  -1 0
                new Thread(
                        () -> {
                            try {
                                InputStream in = client.getInputStream();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                char[] data = new char[1024];
                                while (true) {

                                    int num = reader.read(data);

                                    if (num > 0) {
                                        System.out.println("client read some data is :" + num + " val :" + new String(data, 0, num));
                                    } else if (num == 0) {
                                        System.out.println("client readed nothing!");
                                        continue;
                                    } else {
                                        System.out.println("client readed -1...");
                                        System.in.read();
                                        client.close();
                                        break;
                                    }
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                ).start();


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
