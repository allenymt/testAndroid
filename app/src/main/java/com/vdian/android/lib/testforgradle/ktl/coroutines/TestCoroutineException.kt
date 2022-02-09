package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.*

/**
 * @author yulun
 * @since 2022-02-08 18:24
 * 关于协程的异常处理
 *
 */
class TestCoroutineException {
    // 协程对于异常处理的流程
    // 当一个协程由于异常而运行失败时，它会传播这个异常并传递给它的父协程。接下来，父协程会进行下面几步操作：
    //      取消它自己的子级
    //      取消它自己
    //      将异常传播并传递给它的父级
    //
    //异常会到达层级的根部，且当前 CoroutineScope 所启动的所有协程都会被取消，但协程并非都是一发现异常就执行以上流程，
    // launch 和 async 在处理异常方面有着一些差异
    // launch 将异常视为未捕获异常，类似于 Java 的 Thread.uncaughtExceptionHandler，
    // 当发现异常时就会马上抛出。async 期望最终通过调用 await 来获取结果 (或者异常)，所以默认情况下它不会抛出异常，
    // 这意味着如果使用 async 启动新的协程，它会静默地将异常丢弃，直到调用 async.await() 才会得到目标值或者抛出存在的异常
    object Test {
        private val ioScope = CoroutineScope(Dispatchers.IO)

        fun fetchDocs() {
            // launch的异常会马上抛出
            ioScope.launch {
                delay(500)
                log("taskA throw AssertionError")
                throw AssertionError()
            }

            // async的异常要等await才拿到
            ioScope.async {
                delay(500)
                log("taskA throw AssertionError")
                throw AssertionError()
            }

            // 个人理解是，launch相当于是同步，async是异步，拿flutter参考的话，同步的异常和异步的异常捕获方式也是不一样的
        }


        //CoroutineExceptionHandler
        //如果想主动捕获异常信息，可以使用 CoroutineExceptionHandler 作为协程的上下文元素之一，
        // 在这里进行自定义日志记录或异常处理，它类似于对线程使用 Thread.uncaughtExceptionHandler。
        // 但是，CoroutineExceptionHandler 只会在预计不会由用户处理的异常上调用，因此在 async 中使用它没有任何效果，
        // 当 async 内部发生了异常且没有捕获时，那么调用 async.await() 依然会导致应用崩溃
        fun testCoroutineExceptionHandler() {
            fun main() = runBlocking {
                val handler = CoroutineExceptionHandler { _, exception ->
                    log("Caught $exception")
                }
                val job = GlobalScope.launch(handler) {
                    throw AssertionError()
                }
                val deferred = GlobalScope.async(handler) {
                    throw ArithmeticException()
                }
                // 默认处理的情况，只捕获了launch的异常
                joinAll(job, deferred)

                // 加这一句，不做保护，也会触发async里的异常，因此需要保护
                try {
                    deferred.await()
                } catch (e: Exception) {
                    log(e.toString())
                }
            }
            main()
        }

        // 由于异常导致的取消在协程中是一种双向关系，会在整个协程层次结构中传播，那如果我们需要的是单向取消该怎么实现呢？
        // 例如，假设在 Activity 中启动了多个协程，如果单个协程所代表的子任务失败了，
        // 此时并不一定需要连锁终止整个 Activity 内部的所有其它协程任务，即此时希望子协程的异常不会传播给同级协程和父协程。
        // 而当 Activity 退出后，父协程的异常（即 CancellationException）又应该连锁传播给所有子协程，终止所有子协程
        // 可以使用 SupervisorJob 来实现上述效果，取消操作只会向下传播，一个子协程的运行失败不会影响到同级协程和父协程
        // 以下示例中 firstChild 抛出的异常不会导致 secondChild 被取消，但当 supervisor 被取消时 secondChild 也被同时取消了
        fun testSupervisorJob() {
            fun main() = runBlocking {
//                val supervisor = Job() 普通job的话，当firstChild触发异常被取消后，secondChild也会被取消
                val supervisor = SupervisorJob()
                with(CoroutineScope(supervisor)) {
                    // 没有这个CoroutineExceptionHandler，异常会直接被抛到当前线程中，导致应用崩溃
                    val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
                        log("First child is failing")
                        throw AssertionError("First child is cancelled")
                    }
                    val secondChild = launch {
                        firstChild.join()
                        log("First child is cancelled: ${firstChild.isCancelled}, but second one is still active")
                        try {
                            delay(Long.MAX_VALUE)
                        } finally {
                            log("Second child is cancelled because supervisor is cancelled")
                        }
                    }
                    firstChild.join()
                    log("Cancelling supervisor")
                    //取消所有协程
                    supervisor.cancel()
                    secondChild.join()
                }
            }
            main()

            // 在Android中，任意线程触发的异常，如果不做捕获处理，都会导致应用崩溃
            // 因此在协程里，协程异常不被捕获的话，即使在其他线程，也会导致APP崩溃
//            var newT = Thread(Runnable {
//                throw AssertionError("First child is cancelled")
//            })
//            newT.start()
        }
    }
}