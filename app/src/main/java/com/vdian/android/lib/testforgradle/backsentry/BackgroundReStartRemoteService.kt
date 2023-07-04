package com.vdian.android.lib.testforgradle.backsentry

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.vdian.android.lib.testforgradle.util.LogUtil

/**
 * 后台监听进程
 */
class BackgroundReStartRemoteService : Service() {
    var mHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        LogUtil.Log.log("BackgroundReStartRemoteService onCreate")
        mHandler.postDelayed(Runnable {
            LogUtil.Log.log("start BackgroundReStartService")
            var backgroundSentryIntent =
                Intent(this, BackgroundReStartService::class.java)
            ContextCompat.startForegroundService(this, backgroundSentryIntent)
        }, 1000 * 3)
    }

    override fun onBind(intent: Intent): IBinder {
        LogUtil.Log.log("BackgroundReStartRemoteService onBind")
        return EmptyBinder()
    }

    private fun delayStopSelf(){

    }

}