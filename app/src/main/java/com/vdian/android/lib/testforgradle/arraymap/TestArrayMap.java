package com.vdian.android.lib.testforgradle.arraymap;

import android.support.v4.util.ArrayMap;
import android.support.v4.util.SimpleArrayMap;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yulun
 * @sinice 2020-04-01 21:11
 */
public class TestArrayMap {
    public static void main(String[] args) {
        SimpleArrayMap map = new SimpleArrayMap<String, String>(4);
        map.put("224", "5256");
        testPrintMap(map);
        map.put("222", "5556");
        testPrintMap(map);
        map.put("223", "5156");
        testPrintMap(map);
        map.put("213", "456");
        testPrintMap(map);
    }

    public static void testConcurrent(){
        ConcurrentHashMap<String,String> hashMap = new ConcurrentHashMap<>();
    }
    public static void testPrintMap(SimpleArrayMap map){
        System.out.println("divide line ================================");
        try {
            Class cls = map.getClass();
            Field[] fields = cls.getDeclaredFields();
            Field field = cls.getDeclaredField("mHashes");
            field.setAccessible(true);
            int[] hashs = (int[]) field.get(map);
            for (int i=0;i<hashs.length;i++){
                System.out.println("hash is "+hashs[i]);
            }

            Field field1 = cls.getDeclaredField("mArray");
            field1.setAccessible(true);
            Object[] valueHashs = (Object[]) field1.get(map);
            for (int i=0;i<valueHashs.length;i=i+2){
                System.out.println("hash is "+valueHashs[i] +" value is "+valueHashs[i+1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("divide line ================================");
    }
}
