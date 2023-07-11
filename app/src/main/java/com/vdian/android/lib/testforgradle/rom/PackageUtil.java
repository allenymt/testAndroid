package com.vdian.android.lib.testforgradle.rom;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 包管理器工具类
 *
 * @author xiongxunxiang
 * @since 2020-04-19
 */
public class PackageUtil {
    /**
     * 判断指定app应用是否已安装
     *
     * @param context 上下文
     * @param pkgName app 包名
     * @return 指定app应用是否已安装
     */
    public static boolean isInstalled(@NonNull Context context, String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        // 获取所有已安装程序的包信息
        List<PackageInfo> packages = getInstalledPackages(context);
        for (int i = 0; i < packages.size(); i++) {
            // 循环判断是否存在指定包名
            if (packages.get(i).packageName.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取手机中所有安装的app应用
     *
     * @param context 上下文
     * @return 所有安装的app应用信息
     */
    public static List<PackageInfo> getInstalledPackages(@NonNull Context context) {
        final PackageManager packageManager = context.getPackageManager();
        return packageManager.getInstalledPackages(0);
    }

    public static List<PackageInfo> e(Context var0) {
        int var2 = c();

        List var1;
        try {
            PackageManager var3 = var0.getPackageManager();
            String var4 = Build.MANUFACTURER.toLowerCase();
            if (Build.VERSION.SDK_INT <= 30 || !var4.contains("oppo") && !var4.contains("realme")) {
                var1 = var3.getInstalledPackages(var2);
            } else {
                var1 = a(var0, var2);
            }
        } catch (Throwable var5) {
            var1 = a(var0, var2);
        }
        return var1;
    }

    private static int c() {
        int var0 = 64;
        if (Build.VERSION.SDK_INT >= 28) {
            var0 |= 134217728;
        }

        var0 |= 4;
        return var0;
    }

    private static List<PackageInfo> a(Context var0, int var1) {
        try {
            String[] var2 = new String[]{"pm list package"};
            List var3 = a(var2, 1);
            if (var3 != null && !var3.isEmpty()) {
                ArrayList var4 = new ArrayList();

                for (int var5 = 0; var5 < var3.size(); ++var5) {
                    String var6 = (String) var3.get(var5);
                    if (!TextUtils.isEmpty(var6) && var6.startsWith("package:")) {
                        String var7 = var6.substring(8);
                        if (!TextUtils.isEmpty(var7)) {

                            PackageInfo var8 = new PackageInfo();
                            var8.packageName = var7;

                            var4.add(var8);
                        }
                    }
                }

                return var4;
            } else {
                return null;
            }
        } catch (Throwable var9) {
            return null;
        }
    }

    public static List<String> a(String[] var0, int var1) {
        ArrayList var2 = null;
        if (var1 != 0) {
            var2 = new ArrayList();
        }

        BufferedReader var3 = null;
        InputStreamReader var4 = null;
        Process var5 = null;

        try {
            try {
                var5 = Runtime.getRuntime().exec(var0);
            } catch (Throwable var25) {
                String[] var7 = new String[2 + var0.length];
                String[] var8 = new String[]{"/system/bin/sh", "-c"};
                System.arraycopy(var8, 0, var7, 0, var8.length);
                System.arraycopy(var0, 0, var7, var8.length, var0.length);
                var5 = Runtime.getRuntime().exec(var7);
            }

            if (var1 != 0) {
                var4 = new InputStreamReader(var5.getInputStream());
                var3 = new BufferedReader(var4);

                String var6;
                while ((var6 = var3.readLine()) != null) {
                    var2.add(var6);
                }
            }
        } catch (IOException var26) {
        } catch (Throwable var27) {
        } finally {
            try {
                if (var3 != null) {
                    var3.close();
                }

                if (var4 != null) {
                    var4.close();
                }
            } catch (IOException var24) {
            }

            try {
                if (var5 != null) {
                    var5.exitValue();
                }
            } catch (IllegalThreadStateException var23) {
                var5.destroy();
            }

        }

        return var2;
    }
}
