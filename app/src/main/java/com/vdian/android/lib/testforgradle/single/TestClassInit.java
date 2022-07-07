package com.vdian.android.lib.testforgradle.single;

/**
 * @author yulun
 * @since 2022-06-22 15:45
 */

public class TestClassInit {
    private static TestClassInit singleTon = new TestClassInit();
    public static int count1;
    public static int count2 = 0;

    private TestClassInit() {
        count1++;
        count2++;
    }

    public static TestClassInit getInstance() {
        return singleTon;
    }

    public static class Test {
        public static void main() {
            TestClassInit singleTon = TestClassInit.getInstance();
            android.util.Log.e("TestClassInit","count1=" + singleTon.count1);
            android.util.Log.e("TestClassInit","count2=" + singleTon.count2);
        }
    }
}

