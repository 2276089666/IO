package com.io.socket.selector.multipleSelectorV2;

/**
 * @Author ws
 * @Date 2021/7/8 18:12
 */
public class MainThread {
    public static void main(String[] args) {

        SelectorThreadGroup boss = new SelectorThreadGroup(2,"boss");
        SelectorThreadGroup worker = new SelectorThreadGroup(3,"worker");
        boss.addWorker(worker);
        // boss组监听多个端口
        boss.bind(9090);
        boss.bind(10000);
        boss.bind(10002);


    }
}
