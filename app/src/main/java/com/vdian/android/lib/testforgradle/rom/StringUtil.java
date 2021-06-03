package com.vdian.android.lib.testforgradle.rom;

import android.text.TextUtils;

/**
 * 字符串工具类
 *
 * @author xiongxunxiang
 * @since 2020-04-19
 */
public class StringUtil {

    /**
     * 判断指定字符串是否为数字
     *
     * @param str 数字字符串
     * @return 是否为数字
     */
    public static boolean isNumber(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String formatSize(long size) {
        String unit = "B";
        final int border = 1024;
        float result = size * 1.0F;
        if (result > border) {
            result = result / border;
            unit = "KB";
        }
        if (result > border) {
            result = result / border;
            unit = "MB";
        }
        if (result > border) {
            result = result / border;
            unit = "GB";
        }
        return String.format("%.2f%s", result, unit);
    }

    public static boolean isEmpty(String str) {
        return TextUtils.isEmpty(str) || str.trim().length() <= 0;
    }
}
