package com.vdian.android.lib.testforgradle.self_view

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import com.vdian.android.lib.testforgradle.R

class SelfViewActivity : AppCompatActivity() {
    var selfTestView: View? = null

    var wm: WindowManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_self_view)
        selfTestView = findViewById(R.id.self_red_view)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun onBtnClick(view: View) {
        selfTestView?.animate()?.setDuration(300)?.scaleX(0.5f)?.scaleY(0.5f)?.start()

        var startTime = SystemClock.elapsedRealtimeNanos()

        wm?.defaultDisplay?.getRefreshRate()
        var endTime1 = SystemClock.elapsedRealtimeNanos()
        android.util.Log.i("yulun", "1-${endTime1 - startTime}")
        android.util.Log.i("yulun", "2-${SystemClock.elapsedRealtimeNanos() - endTime1}")
    }
}