package com.vdian.android.lib.testforgradle.hash;

import android.util.LruCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author yulun
 * @sinice 2020-03-22 15:44
 */
public class HashTest {
    public void test(){
//        HashMap<Byte,Integer> sss = new HashMap<>();
//        sss.put(Byte.valueOf((byte)1),1);
//        sss.put(Byte.valueOf((byte)2),2);
//        sss.put(Byte.valueOf((byte)3),3);
//        System.out.println("hash test fist size is "+sss.size());
//
//        sss.entrySet();
//
//        sss.keySet();
//        Collections.synchronizedMap(sss);
//        sss.remove(Byte.valueOf((byte)1));
//        sss.remove(2);
//        System.out.println("hash test two size is "+sss.size());
//
//        Byte bhb =Byte.valueOf((byte)1);
//        Integer aaa = Integer.valueOf(1);
//
//        boolean equal = ((Object)bhb).equals((Object)aaa);
//        System.out.println("hash test hash test is  "+hash(Byte.valueOf((byte)1))+"____"+hash(Integer.valueOf(1).hashCode())+"___equalis "+equal);
//
//        HashMap<Integer,Integer> sss1 = new HashMap<>(1);
//
//        int ddd=16;
//        System.out.println("hash test resize is "+tableSizeFor(ddd));
//
//        Integer i = 99;
//        Integer i1 = 99;
//        Integer i2 = 99;
//
//        Float a1= 99f;
//        Float a2 = 99f;
//        System.out.println("hash test hashcode is i "
//                +i.hashCode()+
//                " i1 "+i1.hashCode()+
//                " i2 "+i2.hashCode()+
//                " a1 "+a1.hashCode()+
//                " a2 "+a2.hashCode());
//
//        for(int ii=0;;++ii){
//            System.out.println("hash test ++i is "+ii);
//            if (ii >= 7)
//                break;
//        }
//
//        for(int ii=0;;ii++){
//            System.out.println("hash test i++ is "+ii);
//            if (ii >= 7)
//                break;
//        }
//        System.out.println("ok1 - System.identityHashCode : " + System.identityHashCode(ok1));

        LruCache<String,String> linkedHashMap = new LruCache<>(5);
        for (int i=0;i<8;i++){
            linkedHashMap.put(String.valueOf(i),i+"value");
        }
        Set<Map.Entry<String,String>> entrySet = linkedHashMap.snapshot().entrySet();
        Iterator<Map.Entry<String,String>> entryIterator=entrySet.iterator();
        while (entryIterator.hasNext()){
            Map.Entry<String,String> entry = entryIterator.next();
            System.out.println("ok1 - entry : " + entry.getKey());
        }

        LruCache<String,String> linkedHashMap1 = new LruCache<>(5);
        for (int i=0;i<8;i++){
            linkedHashMap1.put(String.valueOf(i),i+"value");
        }
        linkedHashMap1.get("2");
        linkedHashMap1.get("4");
        Set<Map.Entry<String,String>> entrySet1 = linkedHashMap1.snapshot().entrySet();
        Iterator<Map.Entry<String,String>> entryIterator1=entrySet1.iterator();
        while (entryIterator1.hasNext()){
            Map.Entry<String,String> entry = entryIterator1.next();
            System.out.println("ok1 - entry : " + entry.getKey());
        }

        TreeMap<String,String> treeMap =  new TreeMap<>();
        for (int i=8;i>=0;i--){
            treeMap.put(String.valueOf(i),i+"value");
        }
        Set<Map.Entry<String,String>> entrySet2 = treeMap.entrySet();
        Iterator<Map.Entry<String,String>> entryIterator2=entrySet2.iterator();
        while (entryIterator2.hasNext()){
            Map.Entry<String,String> entry = entryIterator2.next();
            System.out.println("treemap - entry : " + entry.getKey());
        }
    }

    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n+1;
    }
}
