package com.vdian.android.lib.testforgradle.backsentry

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.vdian.android.lib.testforgradle.util.LogUtil

/**
 * 用于拉起主进程，存活在主进程里
 */
class BackgroundReStartService : Service() {

    override fun onCreate() {
        super.onCreate()
        LogUtil.Log.log("BackgroundReStartService onCreate")
    }

    override fun onBind(intent: Intent): IBinder {
        LogUtil.Log.log("BackgroundReStartService onBind")
       return EmptyBinder()
    }
}

class EmptyBinder : Binder() {}
