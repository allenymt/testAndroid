package com.vdian.android.lib.testforgradle.thread.signle;

/**
 * @author yulun
 * @since 2023-01-16 16:31
 * 多线程测试单例
 */
public class TestSingle {
    private static TestSingle instance;

    private TestSingle() {
    }

    public static void main() {
        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    TestSingle.getInstance();
                }
            });
            t.setName("test Thread + " + i);
            t.start();
        }
    }

    public static TestSingle getInstance() {
        if (instance == null) {
            synchronized (TestSingle.class) {
                if (instance == null) {
                    android.util.Log.e("TestSingle", "getInstance: " + Thread.currentThread().getName());
                    instance = new TestSingle();
                }
            }
        }
        android.util.Log.e("TestSingle", "getInstance2: "+instance);
        return instance;
    }
}
