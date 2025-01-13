package com.vdian.android.lib.testforgradle.ktl.coroutines

import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.ktl.coroutines.flow.TestFlowKotlin
import com.yl.lib.sentry.hook.util.PrivacyUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestCoroutinesActivity : AppCompatActivity() {
    var veiwModel: TestViewModel = TestViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_coroutines)
//        TestCoroutinesBasics.Test.main()
        log("do TestCoroutinesActivity onCreate1")
//        TestCoroutineScope.Test.testSupervisorScope()
//        TestCoroutineBuilder.Test.testAsync()
//        TestCoroutineBuilder.Test.testAsyncError()
//        TestCoroutineContext.Test.testJob()
//        TestCoroutineContext.Test.testDispatchers()
//        TestCoroutineContext.Test.testWithContext()
//        TestCancelCoroutine.Test.testCancel()
//        TestCancelCoroutine.Test.testCancelFail()
//        TestCancelCoroutine.Test.testCancelSuccess()
//        TestCancelCoroutine.Test.testNoCancel()
//        TestCoroutineScope.Test.testDispatcher()
//        TestCoroutineException.Test.fetchDocs()
//        TestCoroutineException.Test.testCoroutineExceptionHandler()
//        TestCoroutineException.Test.testSupervisorJob()
//        TestDebugCoroutine.Test.testThreadRef()



        //以下是flow测试相关
//        TestFlowKotlin.Test.testSuspendSequence()
//        TestFlowKotlin.Test.testSimpleFlow()
//        TestFlowKotlin.Test.testColdFlow()
//        TestFlowKotlin.Test.testCancelFlow()
//        TestFlowKotlin.Test.testContinuityFlow()
//        TestFlowKotlin.Test.testFlowContext()
//        TestFlowKotlin.Test.testWithContextFlow()
        TestFlowKotlin.Test.testChangeThread()
        // lifecycleScope就是SupervisorJob() + Dispatchers.Main.immediate
        lifecycleScope.launch {
            delay(1000)
            log("lifecycleScope launch")
        }
        lifecycle.coroutineScope.launch {
            delay(1000)
            log("coroutineScope launch")
        }
        log("do TestCoroutinesActivity onCreate2")
        veiwModel.user.observe(this, object : Observer<String> {
            override fun onChanged(t: String?) {
                log("veiwModel livedata observe $t")
            }
        })

    }

    override fun onStart() {
        super.onStart()
        log("do TestCoroutinesActivity start")
        veiwModel.fetchDocs()
    }

//    private fun configEpicHook() {
//        var hook = object: XC_MethodHook(){
//            override fun beforeHookedMethod(param: MethodHookParam?) {
//                super.beforeHookedMethod(param)
//
//                android.util.Log.d("TestEpic", "stack= " + PrivacyUtil.Util.getStackTrace() +" , methodName is "+ param?.method?.name)
//
//            }
//        }
//        DexposedBridge.hookAllMethods(TelephonyManager::class.java,"getSubscriberId",hook)
//    }
}