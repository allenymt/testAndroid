package com.vdian.android.lib.testforgradle.proxy;

/**
 * @author yulun
 * @sinice 2020-03-12 22:36
 */
public class ResuleCallbackImp<T> extends ResultCallBack<T> {

    @Override
    T callBack() {
        System.out.println("ResuleCallbackImp");
        realCallBack();
        return null;
    }

    public void realCallBack(){

    }

}
