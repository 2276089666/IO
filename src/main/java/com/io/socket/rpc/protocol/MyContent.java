package com.io.socket.rpc.protocol;

import java.io.Serializable;

/**
 * @Author ws
 * @Date 2021/7/13 14:08
 */
// 发送的message协议的body
public class MyContent implements Serializable {
    String className;
    String methodName;
    Class<?>[] parameterTypes;
    Object[] args;

    // 返回的数据
    Object returnData;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object getReturnData() {
        return returnData;
    }

    public void setReturnData(Object returnData) {
        this.returnData = returnData;
    }
}
