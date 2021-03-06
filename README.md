# IO实战

## 1.Page Cache 和页回写

### 1.1.Page Cache 是什么

>page cache:	页高速缓存是linux内核实现的磁盘缓存.是**通过把磁盘中的数据缓存到物理内存中,把对磁盘的访问变为对物理内存的访问,主要是用来减少对磁盘的I/O操作**.

### 1.2.Page Cache 的地位及原因

1. **地位**: page cache 在**任何**现代操作系统中都尤为重要
2. **原因**:
   - 访问磁盘的速度要远远低于访问内存的速度(ms 和 ns 的差距)
   - 数据一旦被访问,就有可能在短期内再次被访问到.(**局部性原理** p262)如果第一次访问数据就缓存它,那就极有可能在短期内再次被高速缓存命中,由于内存访问比磁盘快得多,所以磁盘的内存缓存 page cache 能给系统的存储性能带来质的飞跃!

### 1.3.写缓存的三种策略

1. nowrite(不缓存): 

   > 不缓存任何写操作

2. write-through cache(写透缓存): 

   >更新缓存,也更新磁盘文件

3. **回写**(linux采用) : 

   >p 263

linux采用的第三种策略

### 1.4.设置回写相关参数

```shell
# 查询,每个参数的意义,回写触发条件,回写暂停条件  p269
[root@localhost ~]# sysctl -a | grep dirty
vm.dirty_background_bytes = 0
vm.dirty_background_ratio = 10
vm.dirty_bytes = 0
vm.dirty_expire_centisecs = 3000
vm.dirty_ratio = 30
vm.dirty_writeback_centisecs = 500
```

在此处修改这些阈值

![image-20210624211856534](README.assets/image-20210624211856534.png)



## 2.java的I/O对比

### 2.1.普通写

```java
// 最普通IO,每循环一次,都要触发系统调用 syscall(用户态切换到内核态) "123456789\n" 写入page cache
    public  void testBasicFileIO() throws IOException, InterruptedException {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while(true){
            Thread.sleep(10);
            out.write(data);
        }
    }
```

>​	直接使用java文件输出流写,而且并没有调用flush,速度是很慢的, 写一段时间直接非正常关闭机器电源发现文件内容为0,也就是全部丢失.因为这段时间里写的内容都被page cache缓存了(上面设置了阈值10%),此时关闭虚拟机page cache的数据没来得及写进磁盘,所以全部丢失,如果写了超过了page cache的阈值,那么数据会被写进磁盘,因为到达阈值触发了写磁盘操作.

### 2.2.缓存写

```java
 // jvm 8KB 的缓冲区,满了,就触发系统调用 syscall ,减少了系统调用,效率提高
    public  void testBufferFileIO() throws IOException, InterruptedException {
        File file = new File(path);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        while (true){
            Thread.sleep(10);
            bos.write(data);
        }
    }
```

>​	使用BufferedOutputStream文件写,速度明显快于直接文件流写,在写的过程中,前面直接文件流写的page cache缓存的页会越来越小,逐渐被写入磁盘,而缓存写会占用越来越大的page cache,当达到阈值会写进磁盘.

**缓存流写比直接文件流写快的原因**:

​	直接文件流写是每调用一次write则将"123456789\n"这10个字节调用一次系统调用write写到page cache,而缓存流写是在JVM里开辟了一个8k 的内存缓冲区,当写满8k时再调用系统调用写到page cache,因为缓存流调用的系统调用次数少,用户态内核态切换次数少,所以要比直接写快的多.

**注:调用flush会强制把page cache脏页写到磁盘.**

### 2.3.NIO(ByteBuffer)

[测试代码](src/main/java/com/io/test/TestByteBuffer.java)

```java
 @Test
    public void testNio() {
        ByteBuffer buffer = ByteBuffer.allocate(1024); // 堆中分配缓冲区
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);  // 直接内存分配缓冲区

        System.out.println("position:\t" + buffer.position());
        System.out.println("limit:\t" +  buffer.limit());
        System.out.println("capacity:\t" + buffer.capacity());

        System.out.println("mark:\t" + buffer);
        System.out.println();

        System.out.println("-------------put:123......");
        buffer.put("123".getBytes());
        System.out.println("mark: " + buffer);
        System.out.println();

        System.out.println("-------------flip......");
        buffer.flip();   //读写交替
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
```

输出结果:

![image-20210625112308798](README.assets/image-20210625112308798.png)

- 缓冲区有三个指针,分别是postition,limit,capacity.初始情况：

![20200712220601990](README.assets/20200712220601990.png)

- pos代表当前位置,limit主要用来记录位置,因为pos会移动,cap代表最大容量.起始时,pos指向起始位置,cap和limit都指向最大位置,当put(“123”)时,会向buffer中加入三个字节,所以pos向右移动了三个在3的位置

  ![20200712220745842](README.assets/20200712220745842.png)

- 当调用buffer.flip()时代表读写交替,从写变为读,pos会移动到起始位置,因为要记录写到哪了,所以limit移动到pos的位置.

  ![20200712220801829](README.assets/20200712220801829.png)

- 当调用buffer.get()时,从buffer中读出一个字节,所以pos会向右移动一个字节.

  ![20200712220822193](README.assets/20200712220822193.png)

- 当调用buffer.compact()时会挤压一下,将已读的清除,将未读的向前挪动,然后pos回到写的位置,limit移动到最后,(当调用带索引的get(index)时,compact不会清除已读,会回到写的位置,limit移动到最后)

  ![20200712220835309](README.assets/20200712220835309.png)

- buffer.clear()将buffer情况,回到起始位置.

  ![20200712220601990](README.assets/20200712220601990.png)



### 2.4.NIO(File)

[测试代码（环境linux）](src/main/java/com/io/test/TestOSFileIO.java)

```java
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

```

>RandomAccessFile随机文件读写,与一般的文件读写不同点在于能够随时调整偏移,也就是seek().
>
>raf.getChannel()能拿到一个可读写的channel,而这个channel能够通过map()方法获取一个mmap的映射,能够直接进入page cache,且不经过系统调用.

### 2.5.io的几种内存分配位置

1. **on heap**:堆上分配,ByteBuffer.allocate(1024),在JVM的堆上分配一个缓冲区,写入时需要先复制到堆外内存,再从堆外复制到page cache,最后写到磁盘.

2. **off heap**:堆外分配（JVM进程内存）,ByteBuffer.allocateDirect(1024);或者使用unsafe.allocateMemory();(unsafe的需要调用freeMemory()回收,ByteBuffer的GC好像能够回收)不在JVM的堆里,而是在Java进程的堆里(jvm的堆只是java堆的一块分配区域,根据参数-Xms和-Xmx决定,区别就是堆内可以直接存储对象,受GC管理,堆外只能使用字节数组,不受GC管理),它相对于jvm堆,少了一步复制到堆外的过程,直接复制到page cache再写到磁盘.

3. **mmap**:raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 4096)。使用RandomAccessFile获取channel可获取到一个mmap的映射缓冲区,直接对接page cache,不产生系统调用.

   ![image-20210625190831431](README.assets/image-20210625190831431.png)

4. 性能：mmap > off heap > on heap

## 3.TCP与Socket

|                                              | 阻塞(读不到写不出时，代码卡在那) | 非阻塞（无论read/write成功与否都可以返回，执行后面代码） |
| -------------------------------------------- | -------------------------------- | -------------------------------------------------------- |
| **同步（app自己完成系统调用read/write）**    | Bio                              | Nio,select.poll,epoll                                    |
| **异步(kernel完成read/write,返回结果给app)** | 无                               | **AIO** : iocp(windows)，linux还没有                     |

### 3.1.Bio

在linux上做实验，深入理解TCP，socket

1. 将代码拷贝到linux,[Server代码](src/main/java/com/io/socket/bio/SocketIOPropertites.java)，[Client代码](src/main/java/com/io/socket/bio/SocketClient.java)

2. 编译我们的.java文件

   ![image-20210625211835657](README.assets/image-20210625211835657.png)

3. 分别开启四个窗口方便我们查看

   - 客户端窗口

     ![image-20210625213851858](README.assets/image-20210625213851858.png)

   - 服务端窗口

     ![image-20210625213910958](README.assets/image-20210625213910958.png)

   - 监听9090端口上的包

     ![image-20210625220628230](README.assets/image-20210625220628230.png)

   - 查看网络状态

     ![image-20210625214214437](README.assets/image-20210625214214437.png)

4. 执行server端的窗口的指令，运行我们的java程序

   ![image-20210625214540326](README.assets/image-20210625214540326.png)

   再次观察我们的网络状态

   ![image-20210625220811869](README.assets/image-20210625220811869.png)

   查看我们5220进程的文件标识符的状态

   ![image-20210625220939148](README.assets/image-20210625220939148.png)

5. 执行client端的窗口的指令，运行我们的java程序

   ![image-20210625215436795](README.assets/image-20210625215436795.png)

   查看9090端口的抓包情况

   ![image-20210625221040506](README.assets/image-20210625221040506.png)

   再次观察我们的网络状况

   ![image-20210625221400850](README.assets/image-20210625221400850.png)

6. 尝试在客户端向服务端发送数据

   ![image-20210625221932367](README.assets/image-20210625221932367.png)

   查看抓包情况

   ![image-20210625222117304](README.assets/image-20210625222117304.png)

   查看网络状态

   ![image-20210625222439205](README.assets/image-20210625222439205.png)

   查看文件标识符

   ![image-20210625222300522](README.assets/image-20210625222300522.png)

7. 查看我们的服务端

   ![image-20210625222603630](README.assets/image-20210625222603630.png)

   放开我们server端的accept()之前的阻塞

   ![image-20210625222638919](README.assets/image-20210625222638919.png)

   查看网络状态

   ![image-20210625222818387](README.assets/image-20210625222818387.png)

   查看文件标识符

   ![image-20210625223135125](README.assets/image-20210625223135125.png)

8. 当我开启多个client时，使用netstat  -natp 查看网络状态，只能有1+2(**配置了： backlog=2**)个socket建立，只要前面的3个连接没断开，再来多少个客户端的socket都无法在内核建立，完成三次握手，和分配资源。

9. 实验结束！！！

#### 3.1.1.实验总结

1. tcp在我们的文件标识符FD分配之前，即accept()调用之前，其实在内核已经建立连接，socket四元组已经创建好了，buffer资源已经分配。

2. 只要四元组（Server IP ,Server Port , Client IP, Client Port）有任何一个维度不一样，我们的socket就可以建立

3. 服务端不需要为每个client分配一个随机端口号，所以只要socket四元组唯一，就能确定哪两个进程在通讯，几百万个客户端连接都没问题

4. 一个进程可以监听多个端口号

   ![image-20210626095455600](README.assets/image-20210626095455600.png)
   
5. tcp三次握手

   ![image-20210627181127687](README.assets/image-20210627181127687.png)

6. tcp得四次挥手

   ![image-20210627174646532](README.assets/image-20210627174646532.png)

   - **client要求断开连接时,server端的连接socket忘记close时**:

     > 会导致server的socket的状态是**close-wait**,client的状态是**fin-wait-2**

   - **tcp四次挥手正常结束时,请求断开连接的方,最后等待2MSL**

     >好处:	防止第四次挥手报文丢失,被要求断开连接方会重发第三次挥手报文,先要求断开连接方还能收到第三次挥手报文,重发第四次挥手报文,并重置2MSL时间等待
     >
     >坏处:	2MSL时间内,server端socket四元组还没释放,消耗了一个client端的socket四元组的连接个数,server端不受影响,受影响的是client(**client是请求断开连接的发起者**)

#### 3.1.2.Socket在内核建立过程

上面的Server端的代码，在内核级别的过程：

![image-20210626114121879](README.assets/image-20210626114121879.png)

**发现每来一个client的连接，Server（或者主线程main）分配不同的fd,新new Thread去拿着分配到的fd去内核的socket四元组的buffer接收数据和发送数据**

### 3.2.Nio

[c10K问题](http://www.kegel.com/c10k.html)

```
描述： 随着互联网的普及，应用的用户群体几何倍增长，此时服务器性能问题就出现。最初的服务器是基于进程/线程模型。新到来一个TCP连接，就需要分配一个进程。假如有C10K，就需要创建1W个进程，可想而知单机是无法承受的。那么如何突破单机性能是高性能网络编程必须要面对的问题，进而这些局限和问题就统称为C10K问题，最早是由Dan Kegel进行归纳和总结的，并且他也系统的分析和提出解决方案。

```

>解决方案：
>
>1. 每个连接分配一个独立的线程/进程（Bio 多线程）
>2. 同一个线程/进程同时处理多个连接  （Nio）

#### 3.2.1.Nio的优缺点

**Nio的三大组件:**

1. Channel : 一般翻译为通道,既可以从通道中读数据,也可以写数据到通道中

   >- FileChannel: 从文件中读写数据
   >- DatagramChannel: 通过UDP读写网络中的数据
   >- SocketChannel: 通过TCP读写网络中的数据
   >- ServerSocketChannel : 可以监听新进来的TCP连接,对每个新来的连接都会创建一个SocketChannel

2. Buffer : 不同数据类型的buffer,例如ByteBuffer , LongBuffer

   >三种分配: 
   >
   >1. 堆上分配
   >2. 直接内存分配
   >3. mmp

3. 多路复用器

[nio代码](src/main/java/com/io/socket/nio/SocketNIO.java)

特点:

```shell
#使用ServerSocketChanne,Channe的accept(),read()等方法不阻塞,我们把建立了连接client丢到list里面,一个线程处理完accept就过来遍历list,看list里面的连接是否有数据需要read/write,没有就跳过,遍历下一个,非阻塞
 public static void main(String[] args) throws Exception {

        LinkedList<SocketChannel> clients = new LinkedList<>();

        ServerSocketChannel ss = ServerSocketChannel.open();  //服务端开启监听：接受客户端
        ss.bind(new InetSocketAddress(9090));
        ss.configureBlocking(false); //OS  NONBLOCKING!!!  保证监听的accept的系统调用是非阻塞的，没人连代码继续往下走


        while (true) {
            //接受客户端的连接
            Thread.sleep(1000);
            // 无论是否有连接，代码都会往下走，不会阻塞
            SocketChannel client = ss.accept(); // 系统调用accept,无连接不阻塞返回-1，java封装成无连接返回NULL


            if (client == null) {
                System.out.println("null.....");
            } else {
                client.configureBlocking(false); // 分配了fd的socket设为非阻塞的，保证接收读取文件的recv是非阻塞的
                int port = client.socket().getPort();
                System.out.println("client..port: " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);  //可以在堆里   堆外

            // 随着连接个数变多，这个集合也会变大，遍历起来也会变慢，连接的创建速度就会变慢
            //遍历已经链接进来的客户端能不能读写数据
            for (SocketChannel c : clients) {   //串行化！！！！  多线程！！
                int num = c.read(buffer);  // >0  -1  0   //不会阻塞
                if (num > 0) {
                    buffer.flip();
                    byte[] aaa = new byte[buffer.limit()];
                    buffer.get(aaa);

                    String b = new String(aaa);
                    System.out.println(c.socket().getPort() + " : " + b);
                    buffer.clear();
                }
            }
        }
    }
```

优点：

```
使用一个或几个线程解决了N个IO连接
```

缺点：

```
在读取recv数据的时候是遍历，很有可能众多连接中只有1到2个buffer中有数据，做了无用功，而且每次读取buffer都是系统调用，有用户态向内核态的切换，效率低
```

#### 3.2.2.OS多路复用器(select,poll)

为了解决上面的Nio的遍历导致时间复杂度O(N)的系统调用,诞生了多路复用器

多路复用器:

>把所有的socket连接的文件标识符FD传给内核,内核去遍历, 返回给app哪些文件标识符可以read或者write的I/O操作,此处只触发了一次用户态到内核态的系统调用,而且还解决了上面遍历导致的没有数据需要读写但还是有系统调用做的无用功

#### 3.2.3.OS多路复用器升级版(epoll)

为了解决select和poll每次都要重复传文件标识符fd给内核,内核针对这个调用最终还会遍历全量fd,由此诞生了epoll

epoll:

>1. server在调用Selector.open()时就会触发epoll_create的系统调用,创建一个文件标识符为fd6的区域(红黑树)去存储我们的所有当前进程的fd,和有状态的fd的链表
>2. server.register()会触发系统调用epoll_ctl把我们的key: fd4   value:EPOLLIN (状态) 添加到红黑树中
>3. 客户端来通过中断创建了一个连接,分配了一个fd7(**图中没有**), 然后调用client.register()内核会重复第2步把key: fd7  value:EPOLLIN (状态)添加到红黑树中
>4. 客户端fd7发送了一些数据,内核中断响应后,会把红黑树中fd7的状态改为有状态(可读或可写),并把fd7 copy到链表中
>5. selector.selectedKeys时,会触发epoll_wait去链表中拿所有有状态的fd,本例fd4(**处理连接accept**),fd7(**处理read/write**)
>6. 我们自己对有状态的fd进行系统调用**处理accept**或**read/write数据**,与此同时内核会更新fd4,fd7的状态
>7. 最终做到epool_wait不存在遍历行为,不用重复传递fd

**注**:select,poll没有在内核开辟空间管理fd,但是jvm会维护一个fd的集合

![image-20210626222234538](README.assets/image-20210626222234538.png)

#### 3.2.4.Java对多路复用器的抽象

java使用一套代码对我们的两种多路复用器来进行操作

````java
package com.io.socket.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * JVM启动参数可以选择os的不同多路复用器的实现,没配置启动参数,JVM优先选择是epoll
 *  -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
 *  -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider
 */
public class SocketMultiplexingSingleThreadV1 {


    private ServerSocketChannel server = null;
    // 多路复用器
    private Selector selector = null;
    int port = 9090;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));


            // 根据jvm的启动参数,如果是epoll则会触发 epoll_create fd6
            selector = Selector.open();

            //server 约等于 listen状态的 fd4
            //register:
            //    如果选用os的select，poll：jvm里开辟一个数组 fd4 放进去
            //    如果选用os的epoll会触发epoll_ctl(fd6,ADD,fd4,EPOLLIN),但是是懒加载,等到select()调用时才触发
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了。。。。。");
        try {
            while (true) {

                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size()+"   size");


                /*
                selector.select()无参的时候当有状态的fd集合为空的时候是阻塞的
                底层调用os的:
                    1，select，poll  其实是内核的select（fd4）  poll(fd4)
                    2，epoll：  其实是内核的 epoll_wait()
                selector.wakeup()  叫醒阻塞的select()
                 */
                while (selector.select() > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();  //返回的有状态的fd集合
                    Iterator<SelectionKey> iter = selectionKeys.iterator();

                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove(); //set  不移除会重复循环处理
                        if (key.isAcceptable()) {
                            //看代码的时候，这里是重点，如果要去接受一个新的连接
                            //语义上，accept接受连接且返回新连接的FD对吧？
                            //那新的FD怎么办？
                            //select，poll，因为他们内核没有空间，那么在jvm中保存和前边的fd4那个listen的一起
                            //epoll： 我们希望通过epoll_ctl把新的客户端fd注册到内核空间的红黑树
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            //连read 还有 write都处理了
                            //在当前线程，这个方法可能会阻塞  ，如果阻塞了十年，其他的IO早就。。。
                            //所以，为什么提出了 IO THREADS
                            //redis  是不是用了epoll，redis是不是有个io threads的概念 ，redis是不是单线程的
                            //tomcat 8,9  异步的处理方式  IO  和   处理上  解耦
                            readHandler(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept(); //来啦，目的是调用accept接受客户端  fd4
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);

            //register:
            //    如果选用os的select，poll：jvm里开辟一个数组 fd5 放进去
            //    如果选用os的epoll会触发epoll_ctl(fd6,ADD,fd5,EPOLLIN)
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadV1 service = new SocketMultiplexingSingleThreadV1();
        service.start();
    }
}
````

[多路复用器单线程版本](src/main/java/com/io/socket/selector/SocketMultiplexingSingleThreadV1.java)

#### 3.2.5.trance追踪java的selector的系统调用

1. 将代码拷贝到对应的环境

   ```shell
   # 生成out.15654的追踪文件
   strace -ff -o out java com.io.socket.selector.SocketMultiplexingSingleThreadV1
   # 显示行号
    :set nu
   # 搜索epoll
    / epoll
   ```

   ![image-20210627155145016](README.assets/image-20210627155145016.png)

同一套代码两个模型的不同系统调用

![image-20210627155421787](README.assets/image-20210627155421787.png)

#### 3.2.6.传统多路复用器造成的问题与改进

1. 由于readHandler代码逻辑复杂耗时长,万一阻塞,或者请求量过大,而程序对所有得fd得处理只有一个线程线性执行,后面的所有的有状态的read/write都得完蛋,所以改用多线程去执行

   [多路复用器多线程代码](src/main/java/com/io/socket/selector/SocketMultiplexingSingleThreadV2.java)

   >为了解决由于异步带来延迟,造成可能重复读写,上面得代码采用
   >
   >```java
   >//系统epoll_ctl(fd6,DELE,fd4,EPOLLIN)调用从红黑树中移除该文件描述符状态
   >key.cancel();
   >```

2. 由于key.cancel();造成系统调用也会有性能损耗,我们改用多个selector执行,每个selector用一个线程线性执行不会有重复消费得问题

   >[混杂模式代码](src/main/java/com/io/socket/selector/multipleSelector/MainThread.java)
   >
   >一个SelectorThreadGroup组里面有多个线程SelectorThread,每个SelectorThread是一个selector,由SelectorThreadGroup选定server listen和client去哪个selector里面注册,SelectorThread有read/write的处理逻辑

3. 创建多个SelectorThreadGroup,接收请求的SelectorThreadGroup我们的boss组专门处理accept,俗称I/O Thread,然后由I/O的SelectorThreadGroup将read/write分配到新的SelectorThreadGroup我们的Worker组,在worker组里面的线程专门处理read/write,将接收和处理分开

   >[多group模式代码](src/main/java/com/io/socket/selector/multipleSelectorV2/MainThread.java)
   >
   >![image-20210709165230296](README.assets/image-20210709165230296.png)
   >
   >代码模仿netty的架构图编写
   >
   ><img src="README.assets/image-20210709153824314.png" alt="image-20210709153824314" style="zoom:80%;" />

### 3.3.Aio

```
由于多路复用器的Nio的selector.select()会阻塞,只有来了一个client请求(连接或read/write)再调用wakeUp解除阻塞.还是会有阻塞
Aio:  纯非阻塞,预埋ReadHandler等,基于Reactor响应式编程,有连接过来就调用我们预埋的Handler,由kernel完成read/write,返回结果给app,
      如果没有连接,程序调用accept()后直接终止,除非自己手动阻塞
```

```java
 public void server() throws IOException {
        /**
         *  带Group的
         */
//        ExecutorService executorService = Executors.newCachedThreadPool();
//        AsynchronousChannelGroup threadGroup = AsynchronousChannelGroup.withCachedThreadPool(executorService, 1);
//
//        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open(threadGroup)
//                .bind(new InetSocketAddress(9090));


        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open()
                .bind(new InetSocketAddress(9090));
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(AsynchronousSocketChannel client, Object attachment) {
                serverChannel.accept(null, this);
                try {
                    System.out.println(client.getRemoteAddress());
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            attachment.flip();
                            System.out.println(new String(attachment.array(), 0, result));
                            client.write(ByteBuffer.wrap("HelloClient".getBytes()));
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            exc.printStackTrace();
                        }
                    });


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });

        // aio纯非阻塞,有连接过来就调用我们预埋的Handler,由kernel完成read/write,返回结果给app,
        // 如果没有连接,程序调用accept()后直接终止,除非自己手动阻塞
        System.in.read();
    }
```

**注意:**Linux还没有kernel完成read/write,返回结果给app的功能,只有windows有,由于java程序主要运行在linux上.

**所以,Netty还是nio使用selector,但是封装成aio一样,预埋handler,基于Reactor模式编程**

[Aio的Server代码](src/main/java/com/io/socket/aio/AioServer.java)

[Aio的Client代码](src/main/java/com/io/socket/aio/AioClient.java)

## 4.Netty

### 4.1.ByteBuf

netty对JDK的bytebuffer进行了封装,有多个指针,不再需要翻转buffer进行读写切换了,还提供了pool(池)化的概念

```java
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
```

[测试代码](src/main/java/com/io/test/TestNettyByteBuf.java)

### 4.2.客户端编写流程

```java
	@Test
    public void client() throws InterruptedException {
        NioSocketChannel client = new NioSocketChannel();

        // pipeline,给出read/write的处理逻辑
        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new ReadWriteHandler());

        // 相当于一个线程池,里面初始化了一个线程,就是我们的selector
        NioEventLoopGroup selector = new NioEventLoopGroup(1);
        // 注册和之前的JDK的方式不一样,之前是client.register(selector,SelectionKey.OP_ACCEPT)
        selector.register(client);


        // 必须先register再连接,并且连接和发送都是异步的,由于得先连接再发送,所以得sync
        ChannelFuture connect = client.connect(new InetSocketAddress("172.16.136.145", 9090));
        connect.sync();


        // write
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server ~".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf);
        send.sync();


        // 用异步感知,是否断开连接
        connect.channel().closeFuture().sync();
        System.out.println("over ...");
    }
```

1. 创建NioSocketChannel就是我们的Socket

2. 利用Reactor 响应式编程的思想,ChannelPipeline addLast(),预埋我们有read/write的操作编写一些业务处理逻辑[ReadWriteHandler](src/main/java/com/io/socket/netty/ReadWriteHandler.java)

3. 创建NioEventLoopGroup,它是一个线程池,每个线程就是一个selector

4. selector.register(client)向我们的多路复用器注册我们的客户端

5. client.connect(new InetSocketAddress("172.16.136.145", 9090)); 采用异步的方式连接server

6. client.writeAndFlush(byteBuf)写内容,发送给server

7. connect.channel().closeFuture().sync()感知是否断开,没断开一直保持连接

   **注意**: 使用[MyChannelInitializer](src/main/java/com/io/socket/netty/MyChannelInitializer.java)类对ReadWriteHandler无法@ChannelHandler.Sharable得窘境解耦

   [使用Neety API手写Client代码](src/main/java/com/io/socket/netty/client/TestNettyClient.java)

### 4.3.服务端编写流程

```java
	@Test
    public void Server() throws InterruptedException {
        NioServerSocketChannel server = new NioServerSocketChannel();

        NioEventLoopGroup selector = new NioEventLoopGroup(1);

        ChannelPipeline pipeline = server.pipeline();
        // AcceptHandler accept接收客户端，并且把client注册到selector,两件事
        // 复用ReadWriteHandler去处理read/write
        pipeline.addLast(new AcceptHandler(new ReadWriteHandler(),selector));

        selector.register(server);

        ChannelFuture bind = server.bind(new InetSocketAddress(9090));
        bind.sync();

        bind.sync().channel().closeFuture().sync();
        System.out.println("server close...");
    }
```



1. 创建NioServerSocketChannel,也就是我们得ServerSocket

2. 创建NioEventLoopGroup,它是一个线程池,每个线程就是一个selector

3. 利用Reactor 响应式编程的思想,ChannelPipeline  addLast() ,预埋我们得[AcceptHandle](src/main/java/com/io/test/AcceptHandler.java)r,去完成accept和register两件事

4. selector.register(client)向我们的多路复用器注册我们的客户端

5. bind

6. 感知bind.sync().channel().closeFuture().sync(),感知是否断开,没断开一直保持连接

   [使用Neety API手写Server代码](src/main/java/com/io/socket/netty/server/TestNettyServer.java)

### 4.4.Netty API方式编写Client

**每行代码逻辑和4.2章得相互呼应**

```java
	@Test
    public void client() throws InterruptedException {
        ChannelFuture connect = new Bootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioSocketChannel.class)
                // 和我的MyChannelInitializer作用类似
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new ReadWriteHandler());
                    }
                })
                .connect(new InetSocketAddress("172.16.136.145", 9090));

        Channel client = connect.sync().channel();

        // write
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello server ~".getBytes());
        ChannelFuture send = client.writeAndFlush(byteBuf);
        send.sync();


        // 用异步感知,是否断开连接
        connect.channel().closeFuture().sync();
        System.out.println("over ...");
    }
```

[代码](src/main/java/com/io/socket/netty/useAPI/NettyClient.java)

### 4.5.Netty API方式编写Server

**每行代码逻辑和4.3章得相互呼应**

```java
 	@Test
    public void server() throws InterruptedException {
        ChannelFuture bind = new ServerBootstrap()
                 //           boss                                worker   
                .group(new NioEventLoopGroup(1),new NioEventLoopGroup(10))
                .channel(NioServerSocketChannel.class)
                // 不需要acceptHandler帮我们处理register了,框架帮我们干了
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast(new ReadWriteHandler());
                    }
                })
                .bind(new InetSocketAddress(9090));

        bind.sync().channel().closeFuture().sync();
        System.out.println("server close...");
    }
```

[代码](src/main/java/com/io/socket/netty/useAPI/NettyServer.java)

## 5.RPC

### 5.1.Client

1. client创建多个线程,每个线程代表一个client
2. 在调用远程方法之前
3. 动态代理生成远程的接口的实现类,自定义消息头header和消息体content变成数据包msgPack
4. 对需要发送的数据编码成byte[]
5. 设置CompletableFuture回调
6. 然后将需要调用某个方法数据包发给server
7. CompletableFuture阻塞client的调用线程,调用完成时,返回得到的值,即方法的返回参数

### 5.2.Server

1. 利用netty的reactor模型接收数据
2. 解码数据
3. 处理数据,得到需要调用的方法,并调用
4. 得到方法得返回值,写回数据

### 5.3.难点

- 设置了client的连接数量,即我们的socket连接  ClientPool(单例)

- 复用我们的client的连接,或创建远程连接,设置相关的handler   ClientFactory(单例)

- 编码 ObjectToByteArray

- 对ChannelPipeline堆积的bytebuf解码,解决粘包问题

  >粘包问题原因:
  >
  >```
  >当多个线程复用一个连接时,即同一个socket连接,当bytebuf的大小不是我们对象的byte[]大小的整数倍时,一定会出现粘包的情况,多个 header body header body header body连在了一起,但是不是一个bytebuf,我们一次只能读取一个bytebuf,会导致某些bytebuf里面的数据缺胳膊少腿,不是一个完整对象,无法实现序列化
  >```
  >
  >![image-20210714152454089](README.assets/image-20210714152454089.png)

  [代码](src/main/java/com/io/socket/rpc/main/ClientMain.java)

