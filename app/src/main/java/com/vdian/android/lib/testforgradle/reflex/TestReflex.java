package com.vdian.android.lib.testforgradle.reflex;

/**
 * @author yulun
 * @sinice 2020-04-24 11:38
 */
public class TestReflex {
    public static final int  abddd= 543;
    final String bbFinal = "bb";
    private final int aaFinal = 1;
    private int aNormal = 2;
    private String absNormal = "abs";

//    {
//        bbFinal = "4";
//        aaFinal = 10;
//    }

//    public TestReflex(String bbFinal, int aaFinal) {
//        this.bbFinal = bbFinal;
//        this.aaFinal = aaFinal;
//    }

    public int getAaFinal() {
        return aaFinal;
    }

    public String getBbFinal() {
        return bbFinal;
    }

    public int getaNormal() {
        return aNormal;
    }

    public void setaNormal(int aNormal) {
        this.aNormal = aNormal;
    }

    public String getAbsNormal() {
        return absNormal;
    }

    public void setAbsNormal(String absNormal) {
        this.absNormal = absNormal;
    }
}
