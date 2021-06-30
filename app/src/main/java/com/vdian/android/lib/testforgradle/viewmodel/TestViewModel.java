package com.vdian.android.lib.testforgradle.viewmodel;

import androidx.lifecycle.ViewModel;

/**
 * @author yulun
 * @sinice 2021-02-23 17:23
 */
public class TestViewModel extends ViewModel {
    int a = 1;

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }
}
