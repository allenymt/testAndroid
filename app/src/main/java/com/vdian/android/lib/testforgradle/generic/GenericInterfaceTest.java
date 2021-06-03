package com.vdian.android.lib.testforgradle.generic;

/**
 * @author yulun
 * @sinice 2020-03-09 10:30
 */
public interface GenericInterfaceTest<T> {

    T testResult();

    T testToChange(T inParam);

    T testToChange1(int inParam);
}
