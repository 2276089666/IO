package com.io.socket.rpc;

import com.io.socket.rpc.util.MsgPack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author ws
 * @Date 2021/7/14 9:15
 */
public class ResponseMappingCallback {
    static ConcurrentHashMap<Long, CompletableFuture> mapping=new ConcurrentHashMap<>();

    public static void addCallBack(Long requestID,CompletableFuture cb){
        mapping.putIfAbsent(requestID,cb);
    }

    public static void removeCallBack(Long requestID){
        mapping.remove(requestID);
    }

    // 将返回的数据,通过发送过去的RequestID,还给对应的CompletableFuture,并删除对应的RequestID的CompletableFuture
    public static void runCallBack(MsgPack msgPack) {
        CompletableFuture future = mapping.get(msgPack.getMyHeader().getRequestID());
        // 远程调用返回的我们需要的方法返回值
        future.complete(msgPack.getMyContent().getReturnData());
        removeCallBack(msgPack.getMyHeader().getRequestID());
    }
}
