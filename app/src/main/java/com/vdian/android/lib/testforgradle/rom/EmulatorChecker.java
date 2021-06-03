package com.vdian.android.lib.testforgradle.rom;

import android.content.Context;

import java.util.Arrays;

/**
 * @author yulun
 * @sinice 2020-05-19 18:51
 * 代码参考自腾讯的bugly
 */
public class EmulatorChecker {

    private static final String[] EMULATOR_ROM_ARRAY = {"google/android_x86", "asus/android_x86"};

    public static boolean isEmulator(Context context) {
        return RomTest.isEmulator(context) || checkEmulator(context);
    }

    public static boolean checkEmulator(Context context) {
        return Arrays.asList(EMULATOR_ROM_ARRAY).contains(romInfo(context)) && isRoot(context);
    }

    public static String romInfo(Context context) {
        String propertyValue;
        if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.miui.ui.version.name")) && !propertyValue.equals("fail")) {
            return "XiaoMi/MIUI/" + propertyValue;
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.build.version.emui")) && !propertyValue.equals("fail")) {
            return "HuaWei/EMOTION/" + propertyValue;
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.lenovo.series")) && !propertyValue.equals("fail")) {
            propertyValue = SystemPropertyHelper.getProperty(context, "ro.build.version.incremental");
            return "Lenovo/VIBE/" + propertyValue;
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.build.nubia.rom.name")) && !propertyValue.equals("fail")) {
            return "Zte/NUBIA/" + propertyValue + "_" + SystemPropertyHelper.getProperty(context, "ro.build.nubia.rom.code");
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.meizu.product.model")) && !propertyValue.equals("fail")) {
            return "Meizu/FLYME/" + SystemPropertyHelper.getProperty(context, "ro.build.display.id");
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.build.version.opporom")) && !propertyValue.equals("fail")) {
            return "Oppo/COLOROS/" + propertyValue;
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.vivo.os.build.display.id")) && !propertyValue.equals("fail")) {
            return "vivo/FUNTOUCH/" + propertyValue;
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.aa.romver")) && !propertyValue.equals("fail")) {
            return "htc/" + propertyValue + "/" + SystemPropertyHelper.getProperty(context, "ro.build.description");
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.lewa.version")) && !propertyValue.equals("fail")) {
            return "tcl/" + propertyValue + "/" + SystemPropertyHelper.getProperty(context, "ro.build.display.id");
        } else if (!StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.gn.gnromvernumber")) && !propertyValue.equals("fail")) {
            return "amigo/" + propertyValue + "/" + SystemPropertyHelper.getProperty(context, "ro.build.display.id");
        } else {
            return !StringUtil.isEmpty(propertyValue = SystemPropertyHelper.getProperty(context, "ro.build.tyd.kbstyle_version")) && !propertyValue.equals("fail") ? "dido/" + propertyValue : SystemPropertyHelper.getProperty(context, "ro.build.fingerprint") + "/" + SystemPropertyHelper.getProperty(context, "ro.build.rom.id");
        }
    }

    public static boolean isRoot(Context context) {
        return RomTest.isRoot(context);
    }

}