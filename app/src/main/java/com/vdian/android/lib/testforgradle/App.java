package com.vdian.android.lib.testforgradle;

import android.app.Application;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.vdian.android.lib.testforgradle.backsentry.AppStatusManager;
import com.vdian.android.lib.testforgradle.backsentry.VivoBackgroundSentry;
import com.vdian.android.lib.testforgradle.room.WordDB;
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
            LogUtil.Log.INSTANCE.log("App onCreate "+Application.getProcessName());
        }
        PrivacySentry.Privacy.INSTANCE.initTransform(this);
        String userAgent = System.getProperty("http.agent");
        Log.i("tstApp", "userAgent is "+userAgent);
        Log.i("tstApp", "ANDROID_ID is "+Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

        String aaa = Build.SERIAL;

        WordDB.Companion.getInstance(this);

        AppStatusManager.getInstance().register(this);

        // 测试进程自启动，验证了不行
//        VivoBackgroundSentry.Companion.getInstance().init(this);
    }
}
