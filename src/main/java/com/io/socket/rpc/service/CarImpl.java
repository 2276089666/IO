package com.io.socket.rpc.service;

/**
 * @Author ws
 * @Date 2021/7/14 10:44
 */
public class CarImpl implements Car{
    @Override
    public String getData(String id) {
        return "server res "+ id;
    }
}
