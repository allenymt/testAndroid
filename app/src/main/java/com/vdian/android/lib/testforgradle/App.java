package com.vdian.android.lib.testforgradle;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.yl.lib.sentry.hook.PrivacySentry;

/**
 * @author yulun
 * @sinice 2021-04-08 11:42
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.i("tstApp", Application.getProcessName());
        }
        PrivacySentry.Privacy.INSTANCE.init(this);
    }
}
