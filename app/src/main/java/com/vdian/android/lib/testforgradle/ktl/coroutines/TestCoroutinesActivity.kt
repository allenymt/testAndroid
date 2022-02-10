package com.vdian.android.lib.testforgradle.ktl.coroutines

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.vdian.android.lib.testforgradle.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestCoroutinesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_coroutines)
//        TestCoroutinesBasics.Test.main()
        log("do TestCoroutinesActivity onCreate1")
        TestCoroutineScope.Test.testSupervisorScope()
//        TestCoroutineBuilder.Test.testAsync()
//        TestCoroutineBuilder.Test.testAsyncError()
//        TestCoroutineContext.Test.testJob()
//        TestCoroutineContext.Test.testDispatchers()
//        TestCoroutineContext.Test.testWithContext()
//        TestCancelCoroutine.Test.testCancel()
//        TestCancelCoroutine.Test.testCancelFail()
//        TestCancelCoroutine.Test.testCancelSuccess()
//        TestCancelCoroutine.Test.testNoCancel()
//        TestCoroutineException.Test.fetchDocs()
//        TestCoroutineException.Test.testCoroutineExceptionHandler()
//        TestCoroutineException.Test.testSupervisorJob()
        lifecycleScope.launch {
            delay(1000)
            log("lifecycleScope launch")
        }
        lifecycle.coroutineScope.launch {
            delay(1000)
            log("coroutineScope launch")
        }
        log("do TestCoroutinesActivity onCreate2")
    }

    override fun onStart() {
        super.onStart()
        log("do TestCoroutinesActivity start")

    }
}