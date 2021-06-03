package com.vdian.android.lib.testforgradle.testSuperClass;

import java.util.UUID;

/**
 * @author yulun
 * @sinice 2020-03-29 16:36
 */
public class TestSuperPatch {
    public static void main(String[] args) {
        TestSuperClass testSuperClass = new TestSuperClass();
        String t = UUID.randomUUID().toString();
        System.out.println("TestSuperPatch UUID " + t);
        testSuperClass.setUuid(t);
        testSuperClass.thisIsSuper();
    }
}
