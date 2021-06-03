package com.vdian.android.lib.testforgradle.testSuperClass;

/**
 * @author yulun
 * @sinice 2020-03-29 16:35
 */
public class SuperClass {
    String uuid;
    public void setUuid(String id) {
        uuid = id;
    }
    public void thisIsSuper() {
       System.out.println("SuperClass thisIsSuper "+uuid);
    }
}
