package com.vdian.android.lib.testforgradle.generic;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yulun
 * @sinice 2020-03-09 10:29
 * 泛型测试
 */
public class GenericTest<T> {
    public T testParam;

    public T getTestParam() {
        return testParam;
    }

    public void setTestParam(T testParam) throws NoSuchMethodException {
//        android.util.Log.i("testGeneric","setTestParam parent");
        // 泛型赋值
        this.testParam = testParam;

        // 测试泛型擦除
        Method method = GenericTest.class.getMethod("getStringList", (Class<?>)null);
        Type returnType = method.getGenericReturnType();
        if(returnType instanceof ParameterizedType){
            ParameterizedType type = (ParameterizedType) returnType;
            Type[] typeArguments = type.getActualTypeArguments();
            for(Type typeArgument : typeArguments){
                Class typeArgClass = (Class) typeArgument;
                System.out.println("typeArgClass = " + typeArgClass);
            }
        }
    }

    protected List<String> stringList = new ArrayList<String>();
    public List<String> getStringList(){
        return this.stringList;
    }
}
