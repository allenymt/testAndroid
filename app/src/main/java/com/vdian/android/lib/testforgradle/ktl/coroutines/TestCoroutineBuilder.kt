package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/**
 * @author yulun
 * @since 2022-02-08 11:34
 */
class TestCoroutineBuilder {
    object Test {
        // 对于launch函数的解释,通过查看launch函数的签名，可以发现就是CoroutineScope的扩展函数，所以调用launch一定要有一个CoroutineScope的实例
        // 那这里为什么能调用呢?test函数实际返回了什么？ launch repeat内部是在哪调用的？
        // 问题1 为什么能调用？ 因为coroutineScope{} 实际上就是lambda表达式，
        // 编译后会自动生成一个类继承自extends SuspendLambda implements Function2，所以test函数实际只是返回了一个coroutineScope
        // 问题2 在问题1已经回答
        // 问题3 launch repeat内部是在哪调用的？ 这个代码还要看编译后生成的类里，可以看下生成后的类名 TestCoroutineBuilder$test$2 实际类名+函数名+数字，实际的调用托管都是在invoke和invokeSuspend里
        // invokeSuspend反编译后看不到源码，所以不清楚
        suspend fun test() {
            coroutineScope {
                // 接着看，launch函数的参数解释
                // launch 函数共包含三个参数：
                //
                //context。用于指定协程的上下文
                //start。用于指定协程的启动方式，默认值为 CoroutineStart.DEFAULT，即协程会在声明的同时就立即进入等待调度的状态，即可以立即执行的状态。可以通过将其设置为CoroutineStart.LAZY来实现延迟启动，即懒加载
                //block。用于传递协程的执行体，即希望交由协程执行的任务
                var xx = launch {
                    repeat(5) {
                        delay(200L * it)
                        log(it)
                    }
                }
                xx.isActive
            }
        }

        // 对于Job (个人理解，句柄就是类似指针的东西)
        // Job 是协程的句柄。使用 launch 或 async 创建的每个协程都会返回一个 Job 实例，该实例唯一标识协程并管理其生命周期。Job 是一个接口类型，这里列举 Job 几个比较有用的属性和函数
        // 每个协程启动后返回的都是一个Job，通过job我们可以知道并控制协程的运行状态
        // https://juejin.cn/post/6908271959381901325#heading-12 协程的运行状态
        fun testJob() {
            //将协程设置为延迟启动
            val job = GlobalScope.launch(start = CoroutineStart.LAZY) {
                for (i in 0..100) {
                    //每循环一次均延迟一百毫秒
                    delay(100)
                }
            }
            job.invokeOnCompletion {
                log("invokeOnCompletion：$it")
            }
            log("1. job.isActive：${job.isActive}")
            log("1. job.isCancelled：${job.isCancelled}")
            log("1. job.isCompleted：${job.isCompleted}")

            job.start()

            log("2. job.isActive：${job.isActive}")
            log("2. job.isCancelled：${job.isCancelled}")
            log("2. job.isCompleted：${job.isCompleted}")

            //休眠四百毫秒后再主动取消协程
            Thread.sleep(400)
            job.cancel(CancellationException("test"))

            //休眠四百毫秒防止JVM过快停止导致 invokeOnCompletion 来不及回调
            Thread.sleep(400)

            log("3. job.isActive：${job.isActive}")
            log("3. job.isCancelled：${job.isCancelled}")
            log("3. job.isCompleted：${job.isCompleted}")
        }

        // 关于async函数
        // async函数也是CoroutineScope的扩展函数，和 launch 的区别主要就在于：async 可以返回协程的执行结果，而 launch 不行
        // 由于 launch 和 async 仅能够在 CouroutineScope 中使用，所以任何创建的协程都会被该 scope 追踪。Kotlin 禁止创建不能够被追踪的协程，从而避免协程泄漏
        fun testAsync() {
            val time = measureTimeMillis {
                runBlocking {
                    val asyncA = async {
                        delay(3000)
                        1
                    }
                    val asyncB = async {
                        delay(4000)
                        2
                    }
                    // await就是在等这个async执行的结果，这里就是输出数据的结果
                    log(asyncA.await() + asyncB.await())
                }
            }
            // 输出整体的耗时，4018ms,会发现小于代码里的两个协程的耗时7s,因为协程是默认启动了，所以整体的实行时间以耗时最大的那个协程为准
            log(time)
        }

        // 这次的执行耗时有7021ms,因为这两个协程的启动方式设置成了lazy
        // 而lazy模式的协程要等start或者await调用后才会执行
        // 针对这个例子，应该是在协程被声明后，马上调用start,await会阻塞，但start不会
        fun testAsyncError(){
            val time = measureTimeMillis {
                runBlocking {
                    val asyncA = async(start = CoroutineStart.LAZY) {
                        delay(3000)
                        1
                    }
                    val asyncB = async(start = CoroutineStart.LAZY) {
                        delay(4000)
                        2
                    }
                    // start函数不会阻塞
//                    asyncA.start()
//                    asyncB.start()
                    log(asyncA.await() + asyncB.await())
                }
            }
            log(time)
        }
    }

}