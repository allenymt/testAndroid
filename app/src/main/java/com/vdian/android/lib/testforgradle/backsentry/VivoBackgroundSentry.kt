package com.vdian.android.lib.testforgradle.backsentry

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.vdian.android.lib.testforgradle.util.LogUtil
import com.yl.lib.sentry.hook.util.MainProcessUtil
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author yulun
 * @since 2022-08-10 15:56
 * 主要解决这个问题 https://bugly.qq.com/v2/crash-reporting/crashes/900001590/3282563/report?pid=1&crashDataType=unSystemExit
 * 方案概述，主进程开启的同时，生成一个子进程，当主进程退到后台时，超过一定时间后，先通知子进程在2s后启动自己，然后kill自己，当子进程再唤起
 * 主进程的service达到后台唤起自己的目的，但这个方案有个问题，就是后台唤起的进程不是前台进程，很快就被杀死
 */
class VivoBackgroundSentry {
    companion object {
        val instance by lazy {
            VivoBackgroundSentry()
        }
    }

    private val bInit: AtomicBoolean = AtomicBoolean(false)
    private var watcher: HandlerThread? = null
    private var watcherHandler: Handler? = null
    private val TAG: String = "VivoBackgroundSentry"
    var context: Context? = null

    @Volatile
    var bBackground: Boolean = false

    fun init(application: Application) {
//        if ("VIVO" != Build.BRAND.toUpperCase()) {
//            return
//        }
        context = application
        if (!MainProcessUtil.MainProcessChecker.isMainProcess(application)) {
            return
        }
        if (bInit.compareAndSet(false, true)) {
            log("init")
            restBackgroundStartTime()
            watcher = HandlerThread("VivoBackgroundSentry")
            AppStatusManager.getInstance()
                .addAppStatusCallback(object : AppStatusManager.AppStatusCallback {
                    override fun onForeground() {
                    }

                    override fun onBackground() {
                    }

                    override fun onApplicationForeground() {
                        bBackground = false
                        log(" bBackground $bBackground")
                        restBackgroundStartTime()
                    }

                    override fun onApplicationBackground() {
                        bBackground = true
                        log(" bBackground $bBackground")
                        postCheckRunnable()
                    }
                })
            watcher?.start()
        }
    }

    private var appBackgroundStartTime: Long = 0
    private val maxOpenNativeCrashTime = 1000 * 10 // 一直在后台超过30分钟
    private val intervalCheckTime: Long = 1000 * 1  // 一分钟检测一次

    private val checkRunnable: Runnable = Runnable {
        try {
            log("do checkRunnable")
            if (appBackgroundStartTime > 0 && SystemClock.elapsedRealtime() - appBackgroundStartTime > maxOpenNativeCrashTime) {
                watcher?.quitSafely()
                watcher?.interrupt()
                var backgroundSentryIntent =
                    Intent(context, BackgroundReStartRemoteService::class.java)
                ContextCompat.startForegroundService(context!!, backgroundSentryIntent)
                log("disable report")
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            } else {
                postCheckRunnable()
                log("check next")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun postCheckRunnable() {
        if (watcherHandler == null) {
            watcherHandler = Handler(watcher?.looper!!)
        }
        if (watcher?.isAlive == true) {
            try {
                watcherHandler?.removeCallbacks(checkRunnable)
                watcherHandler?.postDelayed(checkRunnable, intervalCheckTime) // 1分钟检测一次
            } catch (e: Throwable) {
                e.printStackTrace()
            }

        }

    }

    private fun restBackgroundStartTime() {
        appBackgroundStartTime = SystemClock.elapsedRealtime()
    }

    private fun log(log: String) {
        LogUtil.Log.log("$TAG $log")
    }
}