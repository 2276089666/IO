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

正由于linux采用的第三种策略,才诞生本次的I/O学习

### 1.4.设置回写相关参数

```shell
[root@localhost ~]# sysctl -a | grep dirty
vm.dirty_background_bytes = 0
#脏页达到可用内存的10%就会向磁盘中写,这个参数是后台的,也就是
#内核会有一个线程执行写的过程,比如4个G内存,当到400多M的时候就会写进磁盘
#也就是缓存区占总内存大小
vm.dirty_background_ratio = 10
vm.dirty_bytes = 0
vm.dirty_expire_centisecs = 3000
vm.dirty_ratio = 30
vm.dirty_writeback_centisecs = 500
```





