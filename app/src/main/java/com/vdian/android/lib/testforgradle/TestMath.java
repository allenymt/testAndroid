package com.vdian.android.lib.testforgradle;

/**
 * @author yulun
 * @since 2023-07-13 11:03
 */
public class TestMath {

    public static void main(){
        System.out.println("TestMath main");
        System.out.println("TestMath main 12%15="+getYuShu(12,15));
        System.out.println("TestMath main 15%12="+getYuShu(15,12));
        System.out.println("TestMath main 12/15="+getChuShu(12,15));
        System.out.println("TestMath main 15/12="+getChuShu(15,12));
        System.out.println("TestMath main 15/3="+getChuShu(15,3));
        System.out.println("TestMath main 15%3="+getYuShu(15,3));
    }

    public static int getYuShu(int a,int b){
        return a%b;
    }

    public static int getChuShu(int a,int b){
        return a/b;
    }
}
