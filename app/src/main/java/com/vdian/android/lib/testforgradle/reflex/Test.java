package com.vdian.android.lib.testforgradle.reflex;

import java.net.URI;

/**
 * @author yulun
 * @sinice 2020-03-03 16:32
 */
public class Test {
    private static int c;
    protected int a = 1;
    protected int b = 2;
    private final int d = 3;
    public Test(int a) {
        this.a = a;
    }

//    private static void change(String str) {
//        str = "changed";
//    }
//
//    private static void change(A a) {
//    }
//
//    private static void change1(A a1) {
//        a1.str = "changed";
//    }
//
//    //
//    public static int access000(Test test) {
//        return test.a;
//    }
//
//    public static int access100(Test test) {
//        return test.b;
//    }
//
//    private static void printBBB(String name) {
//        android.util.Log.i("test reflex", name);
//    }

    public int getA() {
        return a;
    }

    private void printxxx(String name) {
        android.util.Log.i("test reflex", name);
    }

    protected  void testGetMethod(){
        System.out.println("Test testGetMethod");
    }
//    public void main() {
//        String str = "hello";
////        change(str);
////        System.out.println(str);
////
////        A a = new A("hello");
////        change(a);
////        System.out.println(a.str);
////
////        A a1 = new A("hello");
////        change1(a1);
////        System.out.println(a1.str);
////
////        int i = 1;
////        change1(i);
////        System.out.println(i);
////        TestSuperPatch.testSuperCall();
////        String url = "https://h5.weidian.com/m/weidian/shop-new.html?shopId=1168523870&referrer=push&0319@spoor=1011.65761.0.1178.newItemFeed#1168523870#realTime#3026160144";
////        test();
////        System.out.println("index 129 is " + url.charAt(129));
////        System.out.println("index 130 is " + url.charAt(130));
////        System.out.println("index 131 is " + url.charAt(131));
////        System.out.println("index 132 is " + url.charAt(132));
////        Test.A c = new B();
////
////        System.out.println("test class Name is  " + Test.class.getName());
////        System.out.println("test c class Name is  " + c.getClass().getName());
//
////        WaitTest.main(null);
//
//        A a = new B();
//        testPrint(a);
//    }
//
//    public void testPrint(A a) {
//        a.test();
//    }
//
//    public void testPrint(B b) {
//        b.test();
//    }
//
//    private void test() {
//        String url = "https://h5.weidian.com/m/weidian/shop-new.html?shopId=1168523870&referrer=push&0319@spoor=1011.65761.0.1178.newItemFeed-1168523870-realTime-3026160144";
//        try {
//            String scheme = URI.create(url.trim()).getScheme();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void change1(int a1) {
////        a1 = a1+1;
//        a1 = 2;
//    }

    public static String testReturnStr(){
        return "123";
    }

    class A {
        public String str;

        public A() {
            str = String.valueOf(a);
            int bb = b;
            bb = 2;
            System.out.println("test class Name is  " + bb);
        }

//    public A(Test test) {
//        Test.access(test);
//    }

        public A(String str) {
            this.str = str;
        }

        public void test() {
            System.out.println("A");
        }

    }

    class B extends A {
        String c;

        @Override
        public void test() {
            System.out.println("B");
        }
    }

}



