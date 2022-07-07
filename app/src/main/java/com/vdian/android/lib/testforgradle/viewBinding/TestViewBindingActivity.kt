package com.vdian.android.lib.testforgradle.viewBinding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.databinding.ActivityTestViewBindingBinding

class TestViewBindingActivity : AppCompatActivity() {
    var activityTestViewBindingBinding: ActivityTestViewBindingBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityTestViewBindingBinding = ActivityTestViewBindingBinding.inflate(layoutInflater)
        setContentView(activityTestViewBindingBinding?.root)
        activityTestViewBindingBinding?.root?.postDelayed({
            activityTestViewBindingBinding?.testViewBind?.text = "helloWord"
        }, 2000)
    }
}