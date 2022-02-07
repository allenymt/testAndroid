package com.vdian.android.lib.testforgradle.ktl.coroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vdian.android.lib.testforgradle.R

class TestCoroutinesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_coroutines)
//        TestCoroutinesBasics.Test.main()
        log("do TestCoroutinesActivity onCreate")
        TestCoroutineScope.Test.testCoroutineScope()
    }

    override fun onStart() {
        super.onStart()
        log("do TestCoroutinesActivity start")
    }
}