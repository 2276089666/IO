package com.io;

import java.io.*;

/**
 * @Author ws
 * @Date 2021/6/24 21:36
 */
public class TestFileIO {

    static  byte[] data = "123456789\n".getBytes();
    static final String path =  "/root/testfileio/out.txt";


    // 最普通IO,每循环一次,都要触发系统调用 syscall(用户态切换到内核态) "123456789\n" 写入page cache
    public static  void testBasicFileIO() throws IOException, InterruptedException {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while(true){
            Thread.sleep(10);
            out.write(data);
        }
    }

    // jvm 8KB 的缓冲区,满了,就触发系统调用 syscall ,减少了系统调用,效率提高
    public static void testBufferFileIO() throws IOException, InterruptedException {
        File file = new File(path);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        while (true){
            Thread.sleep(10);
            bos.write(data);
        }
    }
}
