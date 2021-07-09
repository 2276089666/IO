package com.io.socket.selector.multipleSelector;

/**
 * @Author ws
 * @Date 2021/7/8 18:12
 */
public class MainThread {
    public static void main(String[] args) {
        // 混杂模式,某一个线程不仅server listen,而且还处理selector里的read/write;
        // 其他的线程只处理selector里read/write
        SelectorThreadGroup group = new SelectorThreadGroup(2);
        group.bind(9999);
    }
}
