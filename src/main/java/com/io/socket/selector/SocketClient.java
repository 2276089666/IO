package com.io.socket.selector;

import java.io.*;
import java.net.Socket;


public class SocketClient {

    public static void main(String[] args) {

        try {
            Socket client = new Socket("172.16.136.145",9090);

            client.setSendBufferSize(20);
            client.setTcpNoDelay(true);   // 关闭优化，在优化开启时会积攒超过sendBuffer大小的数据一起发送，某些情况下提高效率
            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while(true){
                String line = reader.readLine();
                if(line != null ){
                    byte[] bb = line.getBytes();
                    for (byte b : bb) {
                        out.write(b);
                    }
                }
                InputStream inputStream = client.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                System.out.println(bufferedReader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
