package com.vdian.android.lib.testforgradle.rom;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yulun
 * @sinice 2021-12-28 17:03
 * 统计
 */
public class AppInstallUtil {
    public static boolean isFirstInstall(Context context) {
        return getPackageFirstInstallTime(context) == getPackageLastUpdateTime(context);
    }

    public static long getPackageFirstInstallTime(Context context) {
        String name = context.getPackageName();
        long time = 0;
        try {
            time = context.getPackageManager().getPackageInfo(name, 0).firstInstallTime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static long getPackageLastUpdateTime(Context context) {
        String name = context.getPackageName();
        long time = 0;
        try {
            time = context.getPackageManager().getPackageInfo(name, 0).lastUpdateTime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static void trackAppInstallTime(Context context) {
        boolean isFirstInstall = isFirstInstall(context);
        long firstInstallTime = getPackageFirstInstallTime(context);
        long lastUpdateTime = getPackageLastUpdateTime(context);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
        Date date = new Date();
        date.setTime(firstInstallTime);
        String firstInstallTimeFormat = dateFormat.format(date);

        date.setTime(lastUpdateTime);
        String lastUpdateTimeFormat = dateFormat.format(date);
        android.util.Log.i("ylTest", "isFirstInstall=" + isFirstInstall + ",firstInstallTime:" + firstInstallTimeFormat + ",lastUpdateTime:" + lastUpdateTimeFormat);
    }
}
