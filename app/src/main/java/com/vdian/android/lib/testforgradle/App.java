package com.vdian.android.lib.testforgradle;

import android.app.Application;
import android.view.WindowManager;

/**
 * @author yulun
 * @sinice 2021-04-08 11:42
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager mWm = getSystemService(WindowManager.class);

    }
}
