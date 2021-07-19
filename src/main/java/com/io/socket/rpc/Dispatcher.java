package com.io.socket.rpc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author ws
 * @Date 2021/7/14 10:01
 */
// 方便我们找到服务端对应接口的实现类
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

    // k: 接口名  v:接口的实现类
    public void register(String k, Object obj) {
        invokeMap.put(k, obj);
    }

    public Object get(String k) {
        return invokeMap.get(k);
    }
}
