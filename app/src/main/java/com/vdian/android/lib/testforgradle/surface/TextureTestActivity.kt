package com.vdian.android.lib.testforgradle.surface

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.vdian.android.lib.testforgradle.databinding.ActivitySurfaceTestBinding
import com.vdian.android.lib.testforgradle.databinding.ActivityTextureTestBinding

class TextureTestActivity : AppCompatActivity() {
    var viewBinding: ActivityTextureTestBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityTextureTestBinding.inflate(layoutInflater)
        setContentView(viewBinding?.root)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }
}