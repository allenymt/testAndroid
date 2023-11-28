package com.vdian.android.lib.testforgradle.go_market;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.room.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * 用于判断Rom包 ；引用 http://www.jianshu.com/p/ba9347a5a05a
 *
 * @author huangyuan
 * @since 18/10/19
 */
public class Rom {


    public static final String ROM_MIUI = "MIUI";
    public static final String ROM_EMUI = "EMUI";
    public static final String ROM_FLYME = "FLYME";
    public static final String ROM_OPPO = "OPPO";
    public static final String ROM_SMARTISAN = "SMARTISAN";
    public static final String ROM_VIVO = "VIVO";
    public static final String ROM_QIKU = "QIKU";
    public static final String ROM_SAMSUNG = "SAMSUNG";

    public static final String BRAND_XIAOMI = "XIAOMI";
    public static final String BRAND_REDMI= "REDMI";
    public static final String BRAND_HUAWEI = "HUAWEI";
    public static final String BRAND_HONOR = "HONOR";
    public static final String BRAND_MEIZU = "MEIZU";
    public static final String BRAND_OPPO = "OPPO";
    public static final String BRAND_SMARTISAN = "SMARTISAN";
    public static final String BRAND_VIVO = "VIVO";
    public static final String BRAND_QIKU = "QIKU";
    public static final String BRAND_SAMSUNG = "SAMSUNG";
    public static final String BRAND_LE_TV = "LETV";
    public static final String BRAND_LENOVO = "LENOVO";
    public static final String BRAND_SONY = "SONY";
    public static final String BRAND_ONEPLUS = "ONEPLUS";
    public static final String BRAND_GOOGLE = "GOOGLE";
    public static final String BRAND_REALME = "REALME";


    private static final String TAG = "Rom";
    private static final String KEY_VERSION_MIUI = "ro.miui.ui.version.name";
    private static final String KEY_VERSION_EMUI = "ro.build.version.emui";
    private static final String KEY_VERSION_OPPO = "ro.build.version.opporom";
    private static final String KEY_VERSION_SMARTISAN = "ro.smartisan.version";
    private static final String KEY_VERSION_VIVO = "ro.vivo.os.version";

    private static String sName;
    private static String sVersion;

    public static boolean isEmui() {
        return check(ROM_EMUI);
    }

    public static boolean isMiui() {
        return check(ROM_MIUI);
    }

    public static boolean isVivo() {
        return check(ROM_VIVO);
    }

    public static boolean isOppo() {
        return check(ROM_OPPO);
    }

    public static boolean isFlyme() {
        return check(ROM_FLYME);
    }

    public static boolean is360() {
        return check(ROM_QIKU) || check("360");
    }

    public static boolean isSmartisan() {
        return check(ROM_SMARTISAN);
    }

    public static boolean isSamsung() {
        return check(ROM_SAMSUNG);
    }

    public static boolean isLeTv() {
        return BRAND_LE_TV.equals(Build.BRAND.toUpperCase());
    }

    public static String getName() {
        if (sName == null) {
            check("");
        }
        return sName;
    }

    public static String getVersion() {
        if (sVersion == null) {
            check("");
        }
        return sVersion;
    }

    public static boolean check(String rom) {
        if (sName != null) {
            return sName.equals(rom);
        }

        if (!TextUtils.isEmpty(sVersion = getProp(KEY_VERSION_MIUI))) {
            sName = ROM_MIUI;
        } else if (!TextUtils.isEmpty(sVersion = getProp(KEY_VERSION_EMUI))) {
            sName = ROM_EMUI;
        } else if (!TextUtils.isEmpty(sVersion = getProp(KEY_VERSION_OPPO))) {
            sName = ROM_OPPO;
        } else if (!TextUtils.isEmpty(sVersion = getProp(KEY_VERSION_VIVO))) {
            sName = ROM_VIVO;
        } else if (!TextUtils.isEmpty(sVersion = getProp(KEY_VERSION_SMARTISAN))) {
            sName = ROM_SMARTISAN;
        } else {
            sVersion = Build.DISPLAY;
            if (sVersion.toUpperCase().contains(ROM_FLYME)) {
                sName = ROM_FLYME;
            } else {
                sVersion = Build.UNKNOWN;
                sName = Build.MANUFACTURER.toUpperCase();
            }
        }
        return sName.equals(rom);
    }

    public static String getProp(String name) {
        String line = null;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + name);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            Log.e(TAG, "Unable to read prop " + name, ex);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }

    /**
     * 针对不同机型
     */
    public static void commitRomVersion() {
        try {
            // 目前 OPPO 三星能获取到
            String romVersion = getProp("ro.build.display.id");

            String romVersion2 = getProp("sys.build.display.id");

            String romVersion3 = getProp("ro.build.software.version");

            String romVersion4 = getProp("ro.build.display.full_id");

            // 华为特殊的渠道
            String romVersion5 = getProp("hwouc.hwpatch.version");

            String brand = Build.BRAND;

            int osVersion = Build.VERSION.SDK_INT;

            StringBuilder sb = new StringBuilder();
            sb.append("romVersion is ").append(romVersion)
                    .append("|romVersion2 is ")
                    .append(romVersion2)
                    .append("|romVersion3 is ")
                    .append(romVersion3)
                    .append("|romVersion4 is ")
                    .append(romVersion4)
                    .append("|romVersion5 is ")
                    .append(romVersion5)
                    .append("|")
                    .append(brand)
                    .append("|")
                    .append(osVersion);

//            // 获得缓存的rom_vesion字段
//            String romVersionCache = FileUtil.getSharedPreferences(AppUtil.getAppContext(), "rom_version_test").getString("rom_cache_version", "");
//
//            boolean romVersionChange;
//
//            if (TextUtils.isEmpty(romVersionCache)) {
//                FileUtil.getSharedPreferences(AppUtil.getAppContext(), "rom_version_test").edit()
//                        .putString("rom_cache_version", sb.toString())
//                        .apply();
//                romVersionChange = false;
//            } else {
//                romVersionChange = !romVersionCache.equals(sb.toString());
//                if (romVersionChange) {
//                    FileUtil.getSharedPreferences(AppUtil.getAppContext(), "rom_version_test").edit()
//                            .putString("rom_cache_version", sb.toString())
//                            .apply();
//                }
//            }
//
//            HashMap<String,String> args = new HashMap<>();
//            args.put("tabletDevice",isTabletDevice()?"1":"0");
//            args.put("foldingScreen",String.valueOf(isFoldingScreen(AppUtil.getAppContext())?"1":"0"));
//            TraceInfo.TraceBuilder builder = new TraceInfo.TraceBuilder()
//                    .setArg1("rom change " + romVersionChange)
//                    .setArg2(sb.toString())
//                    .setArg3("isOhos="+String.valueOf(isOhos()))
//                    .setArgs(args)
//                    .setEventId(9004);
//            WDUT.commitEvent(builder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 判断是否平板
//    private static boolean isTabletDevice() {
//        return (AppUtil.getAppContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
//                Configuration.SCREENLAYOUT_SIZE_LARGE;
//    }

    /**
     * 判断是不是鸿蒙系统
     * @return
     */
    public static boolean isOhos() {
        boolean isOhos = false;
        try {
            Class<?> cls = Class.forName("com.huawei.system.BuildEx");
            Method method = cls.getMethod("getOsBrand");
            isOhos = "harmony".equals(method.invoke(cls));
        } catch (Exception ignored) {
        }
        return isOhos;
    }


    public static final float MAX_WIDTH_HEIGHT = 3f / 4f;
    /**
     * 返回当前设备是否是展开状态折叠屏，目前没有提供准确api，暂用屏幕宽高比判断.
     *
     * @param context 上下文
     * @return 是否是折叠屏展开状态
     */
    public static boolean isFoldingScreen(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        float widthPixels = displayMetrics.widthPixels * 1.0f;
        float heightPixels = displayMetrics.heightPixels * 1.0f;
        return widthPixels / heightPixels > MAX_WIDTH_HEIGHT;
    }
}
