package com.vdian.android.lib.testforgradle.surface

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivitySurfaceNavigationBinding

class SurfaceNavigationActivity : AppCompatActivity() {
    var viewBinding: ActivitySurfaceNavigationBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surface_navigation)
        viewBinding = ActivitySurfaceNavigationBinding.inflate(layoutInflater)
    }

    fun goToTestSurface(view: View?) {
        startActivity(Intent(this, SurfaceTestActivity::class.java))
    }

    fun goToTestCameraTexture(view: View?) {
        startActivity(Intent(this, CameraTextureActivity::class.java))
    }

    fun goToTestCameraSurface(view: View?) {
        startActivity(Intent(this, CameraSurfaceViewShowActivity::class.java))
    }

    fun goToTestCameraTexture2(view: View?) {
        startActivity(Intent(this, CameraTextureViewShowActivity::class.java))
    }

    fun goToTestCameraGlSurface(view: View?) {
        startActivity(Intent(this, CameraGlSurfaceShowActivity::class.java))
    }
}