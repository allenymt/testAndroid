package com.vdian.android.lib.testforgradle;

import android.app.Application;
import android.os.Build;
import android.provider.Settings;
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
        PrivacySentry.Privacy.INSTANCE.initTransform(this);
        String userAgent = System.getProperty("http.agent");
        Log.i("tstApp", "userAgent is "+userAgent);
        Log.i("tstApp", "ANDROID_ID is "+Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

        String aaa = Build.SERIAL;
    }
}
