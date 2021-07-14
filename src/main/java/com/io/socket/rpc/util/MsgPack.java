package com.io.socket.rpc.util;

import com.io.socket.rpc.protocol.MyContent;
import com.io.socket.rpc.protocol.MyHeader;

/**
 * @Author ws
 * @Date 2021/7/14 8:45
 */
// 整个数据包
public class MsgPack {
    private MyHeader myHeader;
    private MyContent myContent;

    public MsgPack(MyHeader myHeader, MyContent myContent) {
        this.myHeader = myHeader;
        this.myContent = myContent;
    }

    public MyHeader getMyHeader() {
        return myHeader;
    }

    public void setMyHeader(MyHeader myHeader) {
        this.myHeader = myHeader;
    }

    public MyContent getMyContent() {
        return myContent;
    }

    public void setMyContent(MyContent myContent) {
        this.myContent = myContent;
    }
}
