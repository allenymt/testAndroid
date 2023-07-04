package com.vdian.android.lib.testforgradle.util;

import android.content.Context;
import android.os.Handler;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * @author yulun
 * @since 2023-07-04 15:34
 */
public class TestUtil {
    public static boolean tryDownCdnMax() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 20 || hour > 22) {
            return false;
        }
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        return random.nextInt(100) % 2 == 0;
    }

    // 测试hash值
    public static void testHash(Handler mHandler) {
        int priInt = 1;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int ut_hash = 0;
                int ut_hash1 = ut_hash + priInt;
                ut_hash = ut_hash | 1;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
                ut_hash = ut_hash | 1 << 1;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
                ut_hash = ut_hash | 1 << 2;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
//                ut_hash = ut_hash | 1 << 3;
//                android.util.Log.i("yulun ut_hash", "ut_hash is "+ ut_hash);
                ut_hash = ut_hash | 1 << 4;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
                ut_hash = ut_hash | 1 << 5;
                android.util.Log.i("yulun ut_hash", "ut_hash is " + ut_hash);
            }
        }, 0);
    }

    // 测试类加载
    public static void testClassLoader(Context context) {
        try {
            context.getClassLoader().loadClass("a.b.c.d.d");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Class.forName("comn.xx.xx.abc");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
