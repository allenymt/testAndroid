package com.vdian.android.lib.testforgradle.single;

/**
 * @author yulun
 * @since 2022-06-22 15:45
 * 一到很有意思的面试题，要给出count1和count2的最终值
 * 考察的是类的初始化顺序
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

