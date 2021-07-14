package com.io.socket.rpc.protocol;

import java.io.Serializable;
import java.util.UUID;

/**
 * @Author ws
 * @Date 2021/7/13 16:22
 */
// 发送message协议的header
public class MyHeader implements Serializable {
    // 协议id
    int flag;
    long requestID;
    long dataLen;

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public long getDataLen() {
        return dataLen;
    }

    public void setDataLen(long dataLen) {
        this.dataLen = dataLen;
    }

   public static MyHeader createHeader(byte[] msg){
        MyHeader myHeader = new MyHeader();

        int f=0x14141414;
        myHeader.setFlag(f);

        int dataLength = msg.length;
        myHeader.setDataLen(dataLength);

        myHeader.setRequestID(Math.abs(UUID.randomUUID().getLeastSignificantBits()));

        return myHeader;
    }
}
