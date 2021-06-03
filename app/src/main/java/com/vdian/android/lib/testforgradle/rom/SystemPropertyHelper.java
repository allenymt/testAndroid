package com.vdian.android.lib.testforgradle.rom;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读取系统类 getprop
 */
public class SystemPropertyHelper {
    private static Map<String, String> systemProperties;

    /**
     * 获取系统的指定属性值，代码参考自腾讯的bugly
     *
     * @param context
     * @param prop    属性名
     * @return 属性值, 比如:0/1, false/true等等
     */
    public static String getProperty(Context context, String prop) {
        if (prop != null && !prop.trim().equals("")) {
            if (systemProperties == null) {
                systemProperties = new HashMap();
                ArrayList properties;
                if ((properties = ShellCmdExecutor.executeCMD(context, "getprop")) != null && properties.size() > 0) {
                    Pattern pattern = Pattern.compile("\\[(.+)\\]: \\[(.*)\\]");
                    Iterator iterator = properties.iterator();

                    while (iterator.hasNext()) {
                        String var3 = (String) iterator.next();
                        Matcher matcher;
                        if ((matcher = pattern.matcher(var3)).find()) {
                            try {
                                systemProperties.put(matcher.group(1), matcher.group(2));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return systemProperties.containsKey(prop) ? systemProperties.get(prop) : "fail";
        } else {
            return "";
        }
    }
}
