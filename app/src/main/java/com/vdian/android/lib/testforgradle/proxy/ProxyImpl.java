package com.vdian.android.lib.testforgradle.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author yulun
 * @sinice 2020-03-13 09:47
 */
public class ProxyImpl {
    public static <T> T getService(Class<T> tClass){
        Object o = Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("ProxyImpl getService the method name is "+method.getName());
                System.out.println("ProxyImpl getService the object  is "+proxy.getClass().getName());
                if (args!=null && args[0]!=null){
                    Object o = args[0];
                    if (o instanceof ResultCallBack){
                        ((ResultCallBack)o).callBack();
                    }
                }
                return null;
            }
        });
        return (T)o;
    }


    public static <T> T getService(final Object object){
        Object o = Proxy.newProxyInstance(object.getClass().getClassLoader(), object.getClass().getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("ProxyImpl getService the method name is "+method.getName());
                System.out.println("ProxyImpl getService the object  is "+proxy.getClass().getName());
                return method.invoke(object,args);
            }
        });
        return (T)o;
    }
}
