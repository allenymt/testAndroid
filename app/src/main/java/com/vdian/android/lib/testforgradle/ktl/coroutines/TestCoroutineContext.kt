package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.*

/**
 * @author yulun
 * @since  15:58
 * CoroutineContext 使用以下元素集定义协程的行为：
Job：控制协程的生命周期, 控制一系列协程的聚合体，job是和context绑定的。
CoroutineDispatcher：将任务指派给适当的线程
CoroutineName：协程的名称，可用于调试
CoroutineExceptionHandler：处理未捕获的异常
 */
class TestCoroutineContext {
    object Test {
        fun testJob() {
            // 定义一个job
            val job = Job()

            // job + Dispatchers.IO生成一个CoroutineContext，然后生成一个scope
            val scope = CoroutineScope(job + Dispatchers.IO)

            //定义一个函数，就像定义一个变量一样
            fun main(): Unit = runBlocking {
                log("job is $job")

                // 这里launch生成的协程和外面定义的job是一对一吗
                // 可以看到连续起了3个Coroutine，之后通过job直接取消的时候，三个是一起取消的
                // job相当于是总控制，协程是子体
                val coroutine1 = scope.launch {
                    try {
                        delay(3000)
                    } catch (e: CancellationException) {
                        log("Coroutine1 is cancelled")
//                        throw e 注释掉throw后，后面的end就会输出了
                    }
                    log("Coroutine1 end")
                }
                coroutine1.cancel()// 协程1取消后，job的isActive=true,
                log("1. job.isActive：${job.isActive} -- coroutine1: ${coroutine1.isActive}")
                log("1. job.isCancelled：${job.isCancelled}-- coroutine1: ${coroutine1.isCancelled}")
                log("1. job.isCompleted：${job.isCompleted}-- coroutine1: ${coroutine1.isCompleted}")

                val coroutine2 = scope.launch {
                    try {
                        delay(3000)
                    } catch (e: CancellationException) {
                        log("Coroutine2 is cancelled")
//                        throw e
                    }
                    log("Coroutine2 end")
                }

                log("2. job.isActive：${job.isActive} -- coroutine2: ${coroutine2.isActive}")
                log("2. job.isCancelled：${job.isCancelled}-- coroutine2: ${coroutine2.isCancelled}")
                log("2. job.isCompleted：${job.isCompleted}-- coroutine2: ${coroutine2.isCompleted}")

                val coroutine3 = scope.launch {
                    try {
                        delay(3000)
                    } catch (e: CancellationException) {
                        log("Coroutine3 is cancelled")
                        throw e
                    }
                    log("Coroutine3 end")
                }
                log("3. job.isActive：${job.isActive} -- coroutine3: ${coroutine3.isActive}")
                log("3. job.isCancelled：${job.isCancelled}-- coroutine3: ${coroutine3.isCancelled}")
                log("3. job.isCompleted：${job.isCompleted}-- coroutine3: ${coroutine3.isCompleted}")
                delay(1000)
                // 这里怎么锁定到是哪个job的 ???
                log("scope job is ${scope.coroutineContext[Job]}")
                scope.coroutineContext[Job]?.cancel()

                log("4. job.isActive：${job.isActive} -- coroutine1: ${coroutine1.isActive}-- coroutine2: ${coroutine2.isActive}-- coroutine2: ${coroutine3.isActive}")
                log("4. job.isCancelled：${job.isCancelled}-- coroutine1: ${coroutine1.isCancelled}-- coroutine2: ${coroutine2.isCancelled}-- coroutine2: ${coroutine3.isCancelled}")
                log("4. job.isCompleted：${job.isCompleted}-- coroutine1: ${coroutine1.isCompleted}-- coroutine2: ${coroutine2.isCompleted}-- coroutine3: ${coroutine3.isCompleted}")
            }
            main()
        }

        //线程调度器 CoroutineDispatcher
        //Kotlin 协程库提供了四个 Dispatcher 用于指定在哪一类线程中执行协程：
        //
        //Dispatchers.Default。默认调度器，适合用于执行占用大量 CPU 资源的任务。例如：对列表排序和解析 JSON
        //Dispatchers.IO。适合用于执行磁盘或网络 I/O 的任务。例如：使用 Room 组件、读写磁盘文件，执行网络请求
        //Dispatchers.Unconfined。对执行协程的线程不做限制，可以直接在当前调度器所在线程上执行
        //Dispatchers.Main。使用此调度程序可用于在 Android 主线程上运行协程，只能用于与界面交互和执行快速工作，例如：更新 UI、调用 LiveData.setValue
        fun testDispatchers() {

            // CoroutinesLog: thread-name:DefaultDispatcher-worker-5 , Default
            // CoroutinesLog: thread-name:DefaultDispatcher-worker-5 , Unconfined 1
            // CoroutinesLog: thread-name:DefaultDispatcher-worker-5 , IO
            // CoroutinesLog: thread-name:DefaultDispatcher-worker-5 , Unconfined 2
            // CoroutinesLog: thread-name:main , Unconfined 3
            // CoroutinesLog: thread-name:DefaultDispatcher-worker-4 , GlobalScope
            // CoroutinesLog: thread-name:main , main runBlocking
            // CoroutinesLog: thread-name:MyOwnThread , newSingleThreadContext
            // CoroutinesLog: thread-name:MyOwnThread , Unconfined 4
            // CoroutinesLog: thread-name:main , do TestCoroutinesActivity start
            // launch 在不执行 Dispatchers 的情况下使用时，它从外部的协程作用域继承上下文和调度器，即和 runBlocking 保持一致，均在 main 线程执行
            // IO 和 Default 均依靠后台线程池来执行
            // Unconfined 则不限定具体的线程类型，当前调度器在哪个线程，就在该线程上进行执行，因此上述例子中每个 Unconfined 协程所在线程均不一样
            // GlobalScope 启动协程时默认使用的调度器是 Dispatchers.Default，因此也是在后台线程池中执行
            // newSingleThreadContext 用于为协程专门创建一个新的线程，专用线程是一种成本非常昂贵的资源，在实际开发时必须当不再需要时释放掉线程资源，或者存储在顶级变量中以便在整个应用程序中进行复用
            fun main() = runBlocking<Unit> {
                //尽管先调用，但是并不是先执行，估计和主线程的looper有关
                launch {
                    log("main runBlocking")
                }

                // Default和io泡在同一个线程上，使用方式不对？？
                launch(Dispatchers.Default) {
                    log("Default")
                    launch(Dispatchers.Unconfined) {
                        log("Unconfined 1") //跟随上一个调度期，也在同一个线程上
                    }
                }
                launch(Dispatchers.IO) {
                    log("IO")
                    launch(Dispatchers.Unconfined) {
                        log("Unconfined 2") //跟随上一个调度期，也在同一个线程上
                    }
                }

                //自定义的线程
                launch(newSingleThreadContext("MyOwnThread")) {
                    log("newSingleThreadContext")
                    launch(Dispatchers.Unconfined) {
                        log("Unconfined 4") //自定义的线程
                    }
                }
                launch(Dispatchers.Unconfined) {
                    log("Unconfined 3") //主线程
                }
                GlobalScope.launch {
                    log("GlobalScope") //GlobalScope默认是跑在default的，所以也在worker线程上
                }
            }
            main()
        }


        // withContext
        // 使用线程池的调度器（例如 Dispatchers.IO 或 Dispatchers.Default）不能保证代码块一直在同一线程上从上到下执行，
        // 在某些情况下，协程在 suspend 和 resume 后可能会将任务移交给另一个线程来执行。这意味着，对于整个 withContext() 块，
        // 由于多线程并发之间的原子性和可见性等原因，先后读取到的线程局部变量可能并非是同个值
        fun testWithContext(){
            // 回顾到之前说的，CoroutineContext的四要素，job ,CoroutineDispatcher,CoroutineName,CoroutineExceptionHandler
            // 如果需要指定多项，用+号拼接即可，CoroutineContext重载了+号操作符
            // TODO CoroutineExceptionHandler
            GlobalScope.launch(CoroutineName("MainCoroutine")+Dispatchers.Main) {
                fetchDocs()
            }
        }
        suspend fun fetchDocs() {                      // Dispatchers.Main
            val result = get("developer.android.com")  // Dispatchers.Main
            log(result)
        }

        suspend fun get(url: String) =                 // Dispatchers.Main
            withContext(Dispatchers.IO) {              // Dispatchers.IO (main-safety block)
                url          // Dispatchers.IO (main-safety block)
            }                                          // Dispatchers.Main

    }
}