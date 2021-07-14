package com.io.socket.rpc.main;

import com.io.socket.rpc.MyProxy;
import com.io.socket.rpc.service.Car;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author ws
 * @Date 2021/7/13 13:01
 */
public class ClientMain {

    @Test
    public void client() {

        AtomicInteger num = new AtomicInteger(0);

        int size = 50;
        Thread[] threads = new Thread[size];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Car car = (Car) Proxy.newProxyInstance(Car.class.getClassLoader(), new Class[]{Car.class}, new MyProxy(Car.class));
                String args = "args" + num.incrementAndGet();
                String res = car.getData(args);
                System.out.println("返回来的参数: " + res + " 发送的参数: " + args);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
