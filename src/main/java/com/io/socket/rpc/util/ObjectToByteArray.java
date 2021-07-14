package com.io.socket.rpc.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @Author ws
 * @Date 2021/7/14 9:40
 */
public class ObjectToByteArray {

    static ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public static synchronized byte[] toByteArray(Object object) {
        baos.reset();
        ObjectOutputStream oos = null;
        byte[] msgBody = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            msgBody= baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return msgBody;
    }
}
