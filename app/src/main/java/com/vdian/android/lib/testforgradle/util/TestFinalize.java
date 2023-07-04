package com.vdian.android.lib.testforgradle.util;

import com.vdian.android.lib.testforgradle.MainActivity;

/**
 * @author yulun
 * @sinice 2020-03-05 15:54
 */
public class TestFinalize {

    /**
     * 当jvm标记了要回收的对象后，会开启1个优先级较低的线程，可能会调用到这个方法，那为什么说可能呢？要注意，这个线程也有可能被杀的
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        android.util.Log.i("testFinalize", "我被回收了");
        MainActivity.quote = this;
    }

    private static int getTestInt(TestFinalize finalize){
        return 1;
    }
}
