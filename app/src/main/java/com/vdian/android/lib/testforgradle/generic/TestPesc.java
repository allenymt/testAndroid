package com.vdian.android.lib.testforgradle.generic;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yulun
 * @sinice 2021-09-14 12:19
 */
class TestPesc {
    public static void testPesc() {
        //通配符 具体泛型 object
        List<Object> list1 = new ArrayList<>();
        list1.add("hello");
        list1.add(0);

        List<String> list2 = new ArrayList<>();
        list2.add("world");

        List<? extends Number> list3 = new ArrayList<Integer>();
        Number a = Integer.valueOf(1);
        Number number = list3.get(0);
        list3.add(null);

        GenericTest<Boolean> c = new GenericTest();
        try {
            c.setTestParam(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
