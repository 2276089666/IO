package com.io.test;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;

/**
 * @Author ws
 * @Date 2021/6/24 21:36
 */
public class TestOSFileIO {

    static byte[] data = "123456789\n".getBytes();
    static final String path = "/root/out.txt";


    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("1: BasicFileIO");
        System.out.println("2: BufferFileIO");
        System.out.println("3: FileNio");
        System.out.println("请输入对应编号：");
        String s = scanner.next();
        switch (s) {
            case "0":
                testBasicFileIO();
                break;
            case "1":
                testBufferFileIO();
                break;
            case "2":
                testFileNio();
            default:
                break;
        }
    }

    // 最普通IO,每循环一次,都要触发系统调用 syscall(用户态切换到内核态) "123456789\n" 写入page cache
    public static void testBasicFileIO() throws IOException, InterruptedException {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while (true) {
            Thread.sleep(10);
            out.write(data);
        }
    }

    // jvm 8KB 的缓冲区,满了,就触发系统调用 syscall ,减少了系统调用,效率提高
    public static void testBufferFileIO() throws IOException, InterruptedException {
        File file = new File(path);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        while (true) {
            Thread.sleep(10);
            bos.write(data);
        }
    }


    public static void testFileNio() throws IOException, InterruptedException {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");

        // out.txt ===> hello RandomAccessFile
        raf.write("hello RandomAccessFile\n".getBytes());
        System.out.println("write------------");
        System.in.read();

        System.out.println("seek---------");
        raf.seek(4);
        // out.txt ===> hellooxxndomAccessFile
        raf.write("ooxx".getBytes());
        System.in.read();


        FileChannel rafchannel = raf.getChannel();
        //mmap  堆外  和文件映射的   byte  not  objtect
        MappedByteBuffer map = rafchannel.map(FileChannel.MapMode.READ_WRITE, 0, 4096);
        System.out.println("map--put--------");
        map.put("@@@".getBytes());  //不是系统调用  但是数据会到达 内核的pagecache
        System.in.read();
        /**
         * 曾经我们是需要out.write()  这样的系统调用，才能让程序的data 进入内核的pagecache
         * 曾经必须有用户态内核态切换
         * mmap的内存映射，依然是内核的pagecache体系所约束的！！！ 换言之，断电丢数据
         *
         * 你可以去github上找一些 其他C程序员写的jni扩展库，使用linux内核的Direct IO
         * 直接IO是忽略linux的pagecache
         * 是把pagecache  交给了程序自己开辟一个字节数组当作pagecache，动用代码逻辑来维护一致性/dirty。。。一系列复杂问题
         */

//        map.force(); //  等价于flush，只有flush了所有page cache上的内存数据才会写入磁盘

        raf.seek(0);

        ByteBuffer buffer = ByteBuffer.allocate(8192);
        rafchannel.read(buffer);   //等价 buffer.put()
        // java.nio.HeapByteBuffer[pos=4096 lim=8192 cap=8192]
        System.out.println(buffer);
        buffer.flip();
        //java.nio.HeapByteBuffer[pos=0 lim=4096 cap=8192]
        System.out.println(buffer);

        for (int i = 0; i < buffer.limit(); i++) {
            Thread.sleep(200);
            // @@@looxxndomAccessFile
            System.out.print(((char) buffer.get(i)));
        }
    }


}
