package com.vdian.android.lib.testforgradle;

import java.lang.reflect.Field;

/**
 * @author yulun
 * @sinice 2020-03-03 23:14
 */
public class InnerClassTest {

    int field1 = 1;
    private int field2 = 2;

    public InnerClassTest() {
        InnerClassA inner = new InnerClassA(new InnerClassTest());
        int v = inner.x2;

    }

    //开放一个静态方法，可以访问这个类的私有变量
    public static int testStaticPrivate(InnerClassTest innerClassTest){
        return innerClassTest.field1;
    }

    public class InnerClassA {
        InnerClassTest this1;

        public InnerClassA(InnerClassTest this1) {
            this.this1 = this1;
        }

        int x1 = field1;
        private int x2 = field2;
    }

    public static class InnerClass {
        int x1 = 0;
        private int x2 = 1;
    }
}
