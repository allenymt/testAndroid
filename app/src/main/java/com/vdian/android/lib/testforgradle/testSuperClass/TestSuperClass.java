package com.vdian.android.lib.testforgradle.testSuperClass;

/**
 * @author yulun
 * @sinice 2020-03-29 16:35
 */
public class TestSuperClass extends SuperClass{
    String subUuid;
    public void setSubUuid(String id) {
        subUuid = id;
    }

    @Override
    public void thisIsSuper() {
        System.out.println("TestSuperClass thisIsSuper no call");
    }
}
