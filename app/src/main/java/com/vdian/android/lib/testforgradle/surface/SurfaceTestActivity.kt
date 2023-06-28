package com.vdian.android.lib.testforgradle.surface

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vdian.android.lib.testforgradle.databinding.ActivitySurfaceTestBinding

class SurfaceTestActivity : AppCompatActivity() {
    var viewBinding: ActivitySurfaceTestBinding? = null
    var testSurfaceView: MySurfaceView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySurfaceTestBinding.inflate(layoutInflater)
        setContentView(viewBinding?.root)
        testSurfaceView = viewBinding?.testSurface
    }

    override fun onResume() {
        super.onResume()
        testSurfaceView?.start()
    }

    override fun onStop() {
        super.onStop()
        testSurfaceView?.stop()
    }
}