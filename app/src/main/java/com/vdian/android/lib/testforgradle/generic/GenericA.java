package com.vdian.android.lib.testforgradle.generic;

/**
 * @author yulun
 * @sinice 2020-03-09 10:37
 */
public class GenericA extends GenericTest<String> {
    @Override
    public String getTestParam() {
        return super.getTestParam();
    }

    @Override
    public void setTestParam(String _testParam) {
//        android.util.Log.i("testGeneric","setTestParam child");
        testParam = _testParam;
    }
}
