package com.vdian.android.lib.testforgradle;

import android.app.Application;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.yl.lib.sentry.hook.PrivacySentry;

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
        PrivacySentry.Privacy.INSTANCE.init(this);
    }
}
