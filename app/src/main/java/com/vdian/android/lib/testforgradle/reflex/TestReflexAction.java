package com.vdian.android.lib.testforgradle.reflex;

import java.lang.reflect.Field;

/**
 * @author yulun
 * @sinice 2020-04-24 11:40
 */
public class TestReflexAction {
    public static void test() {
//        TestReflex testReflex = new TestReflex("bb",10);
        TestReflex testReflex = new TestReflex();

        //test final int
        try {
//            java.lang.String name = "1234";
//            Field strField = java.lang.String.class.getDeclaredField("value");
//            strField.setAccessible(true);
//            char[] data = (char[])strField.get(name);
//            data[4] = 'r';
//            System.out.println("1231231__"+name);

            Field fieldA = TestReflex.class.getDeclaredField("aaFinal");
            fieldA.setAccessible(true);
            fieldA.set(testReflex, 2);
            System.out.println("1231231__"+testReflex.getAaFinal()+"__"+fieldA.get(testReflex));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //test normal int
        try {
            Field fieldA = TestReflex.class.getDeclaredField("aNormal");
            fieldA.setAccessible(true);
            fieldA.set(testReflex, 3);
            System.out.println(testReflex.getaNormal());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //test bbFinal
        try {
            Field fieldA = TestReflex.class.getDeclaredField("bbFinal");
            fieldA.setAccessible(true);
            fieldA.set(testReflex, "sadfsdaf");
            System.out.println("1231231"+testReflex.getBbFinal());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //test absNormal
        try {
            Field fieldA = TestReflex.class.getDeclaredField("absNormal");
            fieldA.setAccessible(true);
            fieldA.set(testReflex, "111sssss");
            System.out.println(testReflex.getAbsNormal());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
