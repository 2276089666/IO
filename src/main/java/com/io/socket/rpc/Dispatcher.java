package com.io.socket.rpc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author ws
 * @Date 2021/7/14 10:01
 */
public class Dispatcher {
    // 单例
    private Dispatcher() {
    }

    private static Dispatcher dis = null;

    static {
        dis = new Dispatcher();
    }

    public static Dispatcher getDispatcher() {
        return dis;
    }

    public static ConcurrentHashMap<String, Object> invokeMap = new ConcurrentHashMap<>();

    public void register(String k, Object obj) {
        invokeMap.put(k, obj);
    }

    public Object get(String k) {
        return invokeMap.get(k);
    }
}
