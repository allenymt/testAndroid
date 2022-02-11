package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.*

/**
 * @author yulun
 * @since 2022-02-08 18:24
 * 关于协程的取消
 */
class TestCancelCoroutine {
    object Test {

        // 这个case还是有参考意义的，注意下面这段话
        // 协程的取消是 协作 的。一段协程代码必须协作才能被取消。 所有 kotlinx.coroutines 中的挂起函数都是 可被取消的 。
        // 它们检查协程的取消， 并在取消时抛出 CancellationException。
        // 下面这个例子里有delay，他是挂起函数，因此它能取消
        fun testCancel() {
            fun main() = runBlocking {
                val job = launch {
                    repeat(1000000000) { i ->
                        log("job: I'm sleeping $i ...")
                        delay(1L)
                    }
                }
                log("main: before delay")
                delay(1300L)
                log("main: I'm tired of waiting!")
                job.cancel()
//                job.join() // 这个case 加不加join都看不出来
                log("main: Now I can quit.")
            }
            main()
        }

        // 取消是协作的, 这句话很重要
        // 协程的取消案例
        // 协程取消并不代表协程真的不运行了，这个和Thread的stop很像
        fun testCancelFail() {
            fun main() = runBlocking {
                val startTime = System.currentTimeMillis()
                val job = launch(Dispatchers.Default) {
                    var nextPrintTime = startTime
                    var i = 0
                    while (i < 5) {
                        if (System.currentTimeMillis() >= nextPrintTime) {
                            log("job: I'm sleeping ${i++} ...")
                            nextPrintTime += 500L
//                            delay(1) //关键在于这个，由于整个计算任务都没有挂起函数参与，因为无法被取消
                        }
                    }
                }
                delay(900L)
                log("main: I'm tired of waiting!")
//                job.cancel() //这里执行完了立马就执行下面的代码了，如果想要卡主等待，就要join
//                job.join() //join和cancelAndJoin都是suspend的，所以能卡主
                job.cancelAndJoin() //这里即使执行了，协程还是会运行，和线程一样，需要在任务里自己判断状态
                log("main: Now I can quit.")
            }
            main()
        }

        //取消协程这个操作类似于在 Java 中调用Thread.interrupt()方法来向线程发起中断请求，
        // 这两个操作都不会强制停止协程和线程，外部只是相当于发起一个停止运行的请求，需要依靠协程和线程响应请求后主动停止运行
        //Java 和 Kotlin 之所以均没有提供一个可以直接强制停止线程或协程的方法，是因为这个操作可能会带来各种意想不到的情况。
        // 例如，在停止线程或协程的时候，它们可能还持有着某些排他性资源（例如：锁，数据库链接），
        // 如果强制性地停止，它们持有的锁就会一直无法得到释放，导致其它线程或协程一直无法得到目标资源，最终就可能导致线程死锁。
        // 所以Thread.stop()方法目前也是处于废弃状态，Java 官方并没有提供一个可靠的停止线程的方法
        fun testCancelSuccess() {
            fun main() = runBlocking {
                val startTime = System.currentTimeMillis()
                val job = launch(Dispatchers.Default) {
                    var nextPrintTime = startTime
                    var i = 0
                    while (i < 5) {
                        if (isActive) { //自行判断当前协程是否结束
                            if (System.currentTimeMillis() >= nextPrintTime) {
                                log("job: I'm sleeping ${i++} ...")
                                nextPrintTime += 500L
                            }
                        } else {
                            return@launch
                        }
                    }
                }
                delay(1300L)
                log("main: I'm tired of waiting!")
                job.cancelAndJoin()
                log("main: Now I can quit.")
            }
            main()
        }

        fun testNoCancel() {
            fun main() = runBlocking {
                log("start")
                val launchA = launch {
                    try {
                        repeat(5) {
                            delay(50)
                            log("launchA-$it")
                        }
                    } finally {
                        delay(50)
                        log("launchA isCompleted")
                    }
                }
                val launchB = launch {
                    try {
                        repeat(5) {
                            delay(50)
                            log("launchB-$it")
                        }
                    } finally {
                        // 这个协程不可取消
                        withContext(NonCancellable) {
                            delay(50)
                            log("launchB isCompleted")
                        }
                    }
                }
                //延时一百毫秒，保证两个协程都已经被启动了
                delay(200)
                launchA.cancel()
                launchB.cancel()
                log("end")
            }
            main()
        }

        //4、父协程和子协程
        //当一个协程在另外一个协程的协程作用域中启动时，它将通过 CoroutineScope.coroutineContext 继承其上下文，
        // 新启动的协程就被称为子协程，子协程的 Job 将成为父协程 Job 的子 Job。父协程总是会等待其所有子协程都完成后才结束自身，
        // 所以父协程不必显式跟踪它启动的所有子协程，也不必使用 Job.join 在末尾等待子协程完成
        //所以虽然 parentJob 启动的三个子协程的延时时间各不相同，但它们最终都会打印出日志
        fun testParentChild(){
            fun main() = runBlocking {
                val parentJob = launch {
                    repeat(3) { i ->
                        launch {
                            delay((i + 1) * 200L)
                            log("Coroutine $i is done")
                        }
                    }
                    log("request: I'm done and I don't explicitly join my children that are still active")
                }
            }
            main()
        }

        //一般情况下，协程的取消操作会通过协程的层次结构来进行传播：如果取消父协程或者父协程抛出异常，那么子协程都会被取消；
        // 而如果子协程被取消，则不会影响同级协程和父协程，但如果子协程抛出异常则也会导致同级协程和父协程被取消
        //对于以下代码，子协程 jon1 被取消并不影响子协程 jon2 和父协程继续运行，但父协程被取消后子协程都会被递归取消

        fun testBroadcastCancel(){
            fun main() = runBlocking {
                val request = launch {
                    val job1 = launch {
                        repeat(10) {
                            delay(300)
                            log("job1: $it")
                            if (it == 2) {
                                log("job1 canceled")
                                cancel()
                            }
                        }
                    }
                    val job2 = launch {
                        repeat(10) {
                            delay(300)
                            log("job2: $it")
                        }
                    }
                }
                delay(1600)
                log("parent job canceled")
                request.cancel()
                delay(1000)
            }
            main()
        }


        //withTimeout
        //withTimeout 函数用于指定协程的运行超时时间，如果超时则会抛出 TimeoutCancellationException，从而令协程结束运行
        //withTimeout方法抛出的 TimeoutCancellationException 是 CancellationException 的子类，
        // 之前我们并未在输出日志上看到关于 CancellationException 这类异常的堆栈信息，这是因为对于一个已取消的协程来说，
        // CancellationException 被认为是触发协程结束的正常原因。但对于withTimeout方法来说，抛出异常是其上报超时情况的一种手段，
        // 所以该异常不会被协程内部消化掉
        fun testTimeOut(){
            fun main() = runBlocking {
                log("start")
                val result = withTimeout(300) {
                    repeat(5) {
                        delay(100)
                    }
                    200
                }
                log(result)
                log("end")
            }
            main()
        }
    }
}