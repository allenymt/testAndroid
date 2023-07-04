package com.vdian.android.lib.testforgradle.thread;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * @author yulun
 * @sinice 2020-03-21 20:52
 */
public class ThreadLocalTest {
    public  void test(){
        ReferenceQueue<Object> objectReferenceQueue= new ReferenceQueue<>();
        TestWeakClass testWeakClass =  new TestWeakClass(new TestWeakKey("123"),new TestWeakKey("123123"),objectReferenceQueue);
        Runtime.getRuntime().gc();
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().gc();
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(Object x ;(x = objectReferenceQueue.poll()) != null;){
            android.util.Log.i("ThreadLocalTest",x.getClass().getSimpleName());
        }

    }

    public  void testWeakHashMap(){
        WeakHashMap<TestWeakKey,String> testMap = new WeakHashMap<>();
        testMap.put(new TestWeakKey("1"),"1");
        Runtime.getRuntime().gc();
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testMap.put(new TestWeakKey("2"),"2");
        Runtime.getRuntime().gc();
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testMap = null;
        Runtime.getRuntime().gc();
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class TestWeakClass extends WeakReference<Object>{
        Object value;

        public TestWeakClass(Object key,TestWeakKey value, ReferenceQueue<? super Object> q) {
            super(key, q);
            this.value = value;
        }
    }

    public static class TestWeakKey{
        String key;

        public TestWeakKey(String key) {
            this.key = key;
        }
    }
}
