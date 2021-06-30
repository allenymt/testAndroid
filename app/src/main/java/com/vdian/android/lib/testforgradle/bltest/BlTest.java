package com.vdian.android.lib.testforgradle.bltest;

import androidx.collection.ArrayMap;

/**
 * @author yulun
 * @sinice 2020-11-11 17:15
 */
class BlTest {
    private final ArrayMap<String, Integer> mTest = new ArrayMap();
    public int has(String id) {
        return mTest.get(id);
    }


//    public boolean has(String id) {
//        return (Boolean)null;
//    }
}
