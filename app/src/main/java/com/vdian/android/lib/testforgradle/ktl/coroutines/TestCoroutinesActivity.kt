package com.vdian.android.lib.testforgradle.ktl.coroutines

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.R

class TestCoroutinesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_coroutines)
//        TestCoroutinesBasics.Test.main()
        log("do TestCoroutinesActivity onCreate")
//        TestCoroutineScope.Test.testSupervisorScope()
//        TestCoroutineBuilder.Test.testAsync()
//        TestCoroutineBuilder.Test.testAsyncError()
//        TestCoroutineContext.Test.testJob()
//        TestCoroutineContext.Test.testDispatchers()
//        TestCoroutineContext.Test.testWithContext()
        TestCancelCoroutine.Test.testCancel()
    }

    override fun onStart() {
        super.onStart()
        log("do TestCoroutinesActivity start")
    }
}