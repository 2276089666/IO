package com.io.socket.rpc;

import com.io.socket.rpc.protocol.MyContent;
import com.io.socket.rpc.transport.ClientFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.CompletableFuture;


/**
 * @Author ws
 * @Date 2021/7/13 13:13
 */
public class MyProxy implements InvocationHandler{

    Class className;
    static Dispatcher dispatcher=Dispatcher.getDispatcher();

    public MyProxy(Class className) {
        this.className = className;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Object res=null;
        Object o = dispatcher.get(className.getName());
        if (o==null){
            MyContent content = new MyContent();
            content.setClassName(className.getName());
            content.setMethodName(method.getName());
            content.setArgs(args);
            content.setParameterTypes(method.getParameterTypes());

            CompletableFuture resF  =ClientFactory.transport(content);
            // 阻塞等待,远程调用完成
            res = resF.get();
        }else {
            // 不是远程请求,本地调用
            Method m = o.getClass().getMethod(method.getName(), method.getParameterTypes());
            System.out.println("Log Proxy  ----- time:"+new Date());
            res=m.invoke(o,args);
        }
        return res;
    }
}
