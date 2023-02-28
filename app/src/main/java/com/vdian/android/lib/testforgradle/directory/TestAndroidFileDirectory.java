package com.vdian.android.lib.testforgradle.directory;

import android.content.Context;
import android.os.Environment;

import static android.os.Environment.DIRECTORY_DCIM;
import static android.os.Environment.DIRECTORY_MUSIC;

/**
 * @author yulun
 * @sinice 2020-11-23 11:28
 * 测试Android各个路径的值
 */
public class TestAndroidFileDirectory {

    public static void testPrintFile(Context context) {
        String getExternalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        String getExternalStorageState = Environment.getExternalStorageState();
        String getExternalStoragePublicDirectoryDCIM = Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).getAbsolutePath();

        String getExternalCacheDir = context.getExternalCacheDir().getAbsolutePath();
        String getExternalFilesDir = context.getExternalFilesDir(DIRECTORY_MUSIC).getAbsolutePath();
        String getFilesDir = context.getFilesDir().getAbsolutePath();
        String getCacheDir = context.getCacheDir().getAbsolutePath();

        android.util.Log.i("AndroidFileDirectory", "getExternalStorageDirectory:" + getExternalStorageDirectory);
        android.util.Log.i("AndroidFileDirectory", "getExternalStorageState:" + getExternalStorageState);

        android.util.Log.i("AndroidFileDirectory", "getExternalStoragePublicDirectoryDCIM:" + getExternalStoragePublicDirectoryDCIM);
        android.util.Log.i("AndroidFileDirectory", "getExternalCacheDir:" + getExternalCacheDir);
        android.util.Log.i("AndroidFileDirectory", "getExternalFilesDir:" + getExternalFilesDir);
        android.util.Log.i("AndroidFileDirectory", "getFilesDir:" + getFilesDir);
        android.util.Log.i("AndroidFileDirectory", "getCacheDir:" + getCacheDir);

    }
}
