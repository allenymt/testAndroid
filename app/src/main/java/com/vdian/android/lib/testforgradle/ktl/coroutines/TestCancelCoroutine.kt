package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author yulun
 * @since 2022-02-08 18:24
 * 关于协程的取消
 */
class TestCancelCoroutine {
    object Test {
        fun testCancel() {
            fun main() = runBlocking {
                val job = launch {
                    repeat(1000) { i ->
                        log("job: I'm sleeping $i ...")
                        delay(100L)
                    }
                }
                delay(1300L)
                log("main: I'm tired of waiting!")
                job.cancel()
//                job.join()
                log("main: Now I can quit.")
            }
            main()
        }
    }
}