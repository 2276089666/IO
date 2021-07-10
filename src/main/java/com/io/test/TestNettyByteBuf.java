package com.io.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.Test;

/**
 * @Author ws
 * @Date 2021/7/10 10:12
 */
public class TestNettyByteBuf {
    @Test
    public void TestByteBufOnDirect(){
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(8, 20);
        print(buffer);
        buffer.writeBytes(new byte[]{1,2,3,4});
        print(buffer);
        buffer.writeBytes(new byte[]{1,2,3,4});
        print(buffer);
        // capacity超过8,在20以内双倍扩容,变成16
        buffer.writeBytes(new byte[]{1,2,3,4});
        print(buffer);
        buffer.writeBytes(new byte[]{1,2,3,4});
        print(buffer);
        buffer.writeBytes(new byte[]{1,2,3,4});
        print(buffer);
        // 此时byteBuf已满,数组下标越界
        buffer.writeBytes(new byte[]{1,2,3,4});
        print(buffer);
    }

    @Test
    public void TestByteBufOnHeapBuffer(){
        ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer(8, 20);
        print(buffer);
    }

    @Test
    public void TestByteBufByPool(){
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(8, 20);
        print(buffer);
    }

    public void print(ByteBuf buf){
        System.out.println("buf.isReadable()    :"+buf.isReadable());
        System.out.println("buf.readerIndex()   :"+buf.readerIndex());
        System.out.println("buf.readableBytes() :"+buf.readableBytes());
        System.out.println("buf.isWritable()    :"+buf.isWritable());
        System.out.println("buf.writerIndex()   :"+buf.writerIndex());
        System.out.println("buf.writableBytes() :"+buf.writableBytes());
        System.out.println("buf.capacity()      :"+buf.capacity());
        System.out.println("buf.maxCapacity()   :"+buf.maxCapacity());
        System.out.println("buf.isDirect()      :"+buf.isDirect());
        System.out.println("=================================");

    }

}
