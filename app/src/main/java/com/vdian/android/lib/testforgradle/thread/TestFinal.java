package com.vdian.android.lib.testforgradle.thread;

/**
 * @author yulun
 * @sinice 2020-03-19 20:29
 */
public class TestFinal {
    final int x;
    int y;

    public TestFinal() {
        x =2;
    }

    public TestFinal(int y) {
        this.y = y;
        x=1;
    }
}
