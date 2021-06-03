package com.vdian.android.lib.testforgradle.rom;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * @author yulun
 * @sinice 2020-05-19 18:5
 * 内存memory工具 ，参考自腾讯bugly
 */
public class MemoryUtil {

    /**
     * 是否内存不足
     *
     * @param context
     * @return 当前内存是否不足
     */
    public static boolean isLowMemory(Context context) {
        if (context == null) {
            return false;
        } else {
            ActivityManager a = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

            try {
                ActivityManager.MemoryInfo var2 = new ActivityManager.MemoryInfo();
                a.getMemoryInfo(var2);
                return var2.lowMemory;
            } catch (Throwable var1) {
                var1.printStackTrace();
                return false;
            }
        }
    }

    /**
     * 获取内存大小
     *
     * @param context
     * @return
     */
    public static String getMemorySize(@NonNull Context context) {
        long freeRamMemorySize = freeRamMemorySize(context);
        long totalRamMemorySize = totalRamMemorySize(context);
        return String.format("%dM/%dM", freeRamMemorySize, totalRamMemorySize);
    }

    public static long freeRamMemorySize(@NonNull Context context) {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return 0;
            }
            activityManager.getMemoryInfo(mi);
            long availableMegs = mi.availMem / 1048576L;//M
            return availableMegs;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long totalRamMemorySize(@NonNull Context context) {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return 0;
            }
            activityManager.getMemoryInfo(mi);
            long availableMegs = mi.totalMem / 1048576L;//M
            return availableMegs;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }
}
