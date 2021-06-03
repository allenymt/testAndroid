package com.vdian.android.lib.testforgradle.single;

/**
 * @author yulun
 * @sinice 2020-09-22 14:14
 */
public class TestStaticInnerSingle {
    public TestStaticInnerSingle() {
        android.util.Log.i("TestInner", "TestStaticInnerSingle init");
    }

    public static TestStaticInnerSingle getInstance() {

        return TestInner.instance;
    }

    private static class TestInner {
        private static TestStaticInnerSingle instance = new TestStaticInnerSingle();

        public TestInner() {
            android.util.Log.i("TestInner", "TestInner init");
        }
    }
}
