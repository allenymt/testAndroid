package com.vdian.android.lib.testforgradle.rom;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

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
}
