package com.io.socket.rpc.transport;

import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @Author ws
 * @Date 2021/7/13 16:38
 */
// client的连接池,允许同时有多少个连接
public class ClientPool {
    NioSocketChannel[] clients;
    Object[] locks;

    int maxClientSize;

    public ClientPool(int maxClientSize) {
        this.maxClientSize=maxClientSize;
        clients=new NioSocketChannel[maxClientSize];
        locks=new Object[maxClientSize];
        for (int i = 0; i < clients.length; i++) {
            clients[i]=new NioSocketChannel();
        }
        for (int i = 0; i < locks.length; i++) {
            locks[i]=new Object();
        }
    }

    public int getMaxClientSize() {
        return maxClientSize;
    }
}
