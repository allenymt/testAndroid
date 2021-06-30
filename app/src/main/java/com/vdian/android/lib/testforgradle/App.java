package com.vdian.android.lib.testforgradle;

import android.app.Application;
import android.os.Build;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

/**
 * @author yulun
 * @sinice 2021-04-08 11:42
 */
public class App extends Application {

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.i("tstApp", Application.getProcessName());
        WindowManager mWm = getSystemService(WindowManager.class);

    }
}
