package com.vdian.android.lib.testforgradle.memory;

import android.app.ActivityManager;
import android.content.Context;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * @author yulun
 * @sinice 2020-11-25 21:11
 */
public class TestMemory {
    public static void testMemory(Context context){
        android.util.Log.i("yulunMemory",Runtime.getRuntime().maxMemory()/1024/1024+" ");
        ActivityManager ams =  (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        android.util.Log.i("yulunMemory","getMemoryClass:"+ams.getMemoryClass()+" ");
        android.util.Log.i("yulunMemory","getLargeMemoryClass:"+ams.getLargeMemoryClass()+" ");
    }
}
