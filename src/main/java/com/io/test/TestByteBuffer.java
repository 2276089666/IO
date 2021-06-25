package com.io.test;

import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @Author ws
 * @Date 2021/6/25 18:14
 */
public class TestByteBuffer {
    @Test
    public void testNio() {
        ByteBuffer buffer = ByteBuffer.allocate(1024); // 堆中分配缓冲区
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);  // 直接内存分配缓冲区

        System.out.println("position:\t" + buffer.position());
        System.out.println("limit:\t" + buffer.limit());
        System.out.println("capacity:\t" + buffer.capacity());

        System.out.println("mark:\t" + buffer);
        System.out.println();

        System.out.println("-------------put:123......");
        buffer.put("123".getBytes());
        System.out.println("mark: " + buffer);
        System.out.println();

        System.out.println("-------------flip......");
        buffer.flip();//读写交替
        System.out.println("mark: " + buffer);
        System.out.println();

        System.out.println("-------------get......");
        buffer.get();
        System.out.println("mark: " + buffer);
        System.out.println();

        System.out.println("-------------compact......");
        buffer.compact();
        System.out.println("mark: " + buffer);
        System.out.println();

        System.out.println("-------------clear......");
        buffer.clear();
        System.out.println("mark: " + buffer);
        System.out.println();
    }
}
