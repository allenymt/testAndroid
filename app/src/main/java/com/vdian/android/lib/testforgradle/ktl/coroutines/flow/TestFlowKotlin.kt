package com.vdian.android.lib.testforgradle.ktl.coroutines.flow

import com.vdian.android.lib.testforgradle.ktl.coroutines.log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * @author yulun
 * @since 2022-02-11 15:18
 */
class TestFlowKotlin {
    object Test {
        fun testSequence() {
            fun simple(): Sequence<Int> = sequence { // 序列构建器
                for (i in 1..3) {
                    Thread.sleep(100) // 假装我们正在计算
                    yield(i) // 产生下一个值,yield这个单词本身就是生产，产生的意思
                }
            }

            fun main() {
                simple().forEach { value -> log(value) }
            }
            main()
        }

        fun testSuspendSequence() {
            suspend fun simple(): List<Int> {
                delay(1000) // 假装我们在这里做了一些异步的事情
                return listOf(1, 2, 3)
            }

            fun main() = runBlocking<Unit>(Dispatchers.IO) {
                simple().forEach { value -> log(value) }
            }

            main()
        }

        //注意使用 Flow 的代码与先前示例的下述区别：
        //
        //名为 flow 的 Flow 类型构建器函数。
        //flow { ... } 构建块中的代码可以挂起。
        //函数 simple 不再标有 suspend 修饰符。
        //流使用 emit 函数 发射 值。
        //流使用 collect 函数 收集 值。
        //以下打印结果都在主线程
        //I'm not blocked 1
        //1
        //I'm not blocked 2
        //2
        //I'm not blocked 3
        //3
        // 注意看打印结果，
        fun testSimpleFlow() {
            fun simple(): Flow<Int> = flow { // 流构建器
                for (i in 1..3) {
                    //切换到sleep后，打印的数据变成了 1 ，2， 3，I'm not blocked 1，I'm not blocked 2，I'm not blocked 3
                    //也就是流先执行完了，阻塞了主线程？？
//                    Thread.sleep(100)
                    delay(100) // 假装我们在这里做了一些有用的事情，delay是非阻塞的
                    emit(i) // 发送下一个值
                }
            }

            fun main() = runBlocking<Unit> {
                // 启动并发的协程以验证主线程并未阻塞
                launch {
                    for (k in 1..3) {
                        log("I'm not blocked $k")
                        delay(100)
                    }
                }
                // 收集这个流
                simple().collect { value -> log(value) }
            }
            main()
        }

        // 流是冷的
        // flow在执行构造的时候才会执行，也就是执行collect函数
        //Calling simple function...
        //Calling collect...
        //Flow started
        //1
        //2
        //3
        //Calling collect again...
        //Flow started
        //1
        //2
        //3
        //下面这段话该怎么理解呢？
        //这是返回一个流的 simple 函数没有标记 suspend 修饰符的主要原因。
        //通过它自己，simple() 调用会尽快返回且不会进行任何等待。
        // 该流在每次收集的时候启动， 这就是为什么当我们再次调用 collect 时我们会看到“Flow started”。
        fun testColdFlow() {
            fun simple(): Flow<Int> = flow {
                log("Flow started")
                for (i in 1..3) {
                    delay(100)
                    emit(i)
                }
            }

            fun main() = runBlocking<Unit> {
                log("Calling simple function...")
                val flow = simple()
                log("Calling collect...")
                flow.collect { value -> log(value) }
                log("Calling collect again...")
                flow.collect { value -> log(value) }
            }
            main()
        }

        //如何主动取消一个流呢?
        //这个demo里是借用了withTimeoutOrNull来实现
        //流采用与协程同样的协作取消。像往常一样，流的收集可以在当流在一个可取消的挂起函数（例如 delay）中挂起的时候取消。
        // 以下示例展示了当 withTimeoutOrNull 块中代码在运行的时候流是如何在超时的情况下取消并停止执行其代码的：
        fun testCancelFlow() {

            fun simple(): Flow<Int> = flow {
                for (i in 1..3) {
                    delay(100)
                    log("Emitting $i")
                    emit(i)
                }
            }

            fun main() = runBlocking<Unit> {
                withTimeoutOrNull(250) { // 在 250 毫秒后超时
                    var tstFlow = simple()
                    tstFlow.collect { value -> log(value) }
                }
                log("Done")


                //主动取消流
                (1..5).asFlow().cancellable().collect { value ->
                    if (value == 3) cancel()
                    println(value)
                }
            }
            main()



        }

        //过渡流操作符
        // 这特么不就是rx吗
        fun testFlowTrans() {
            suspend fun performRequest(request: Int): String {
                delay(1000) // 模仿长时间运行的异步工作
                return "response $request"
            }

            fun main() = runBlocking<Unit> {
                (1..3).asFlow() // 一个请求流
                    .map { request -> performRequest(request) }
                    .collect { response -> log(response) }


                (1..3).asFlow() // 一个请求流
                    .transform { request ->
                        emit("Making request $request")
                        emit(performRequest(request))
                    }.collect { response -> log(response) }
            }
            main()
        }

        //流是连续的
        //  CoroutinesLog: thread-name:main , Filter 1
        //  CoroutinesLog: thread-name:main , Filter 2
        //  CoroutinesLog: thread-name:main , Map 2
        //  CoroutinesLog: thread-name:main , Collect string 2
        //  CoroutinesLog: thread-name:main , Filter 3
        //  CoroutinesLog: thread-name:main , Filter 4
        //  CoroutinesLog: thread-name:main , Map 4
        //  CoroutinesLog: thread-name:main , Collect string 4
        //  CoroutinesLog: thread-name:main , Filter 5
        fun testContinuityFlow() {
            runBlocking {
                (1..5).asFlow()
                    .filter {
                        log("Filter $it")
                        it % 2 == 0
                    }
                    .map {
                        log("Map $it")
                        "string $it"
                    }.collect {
                        log("Collect $it")
                    }
            }
        }

        //流上下文,流的运行依托于所在的协程或者线程
        fun testFlowContext() {
            fun simple(): Flow<Int> = flow {
                log("Started simple flow")
                for (i in 1..3) {
                    delay(1000L)
                    emit(i)
                }
            }

            fun main() = runBlocking<Unit>(Dispatchers.IO) {
                simple().collect { value -> log("Collected $value") }
            }
            main()
        }

        //withContext 发出错误
        //长时间运行的消耗 CPU 的代码也许需要在 Dispatchers.Default 上下文中执行，并且更新 UI 的代码也许需要在 Dispatchers.Main 中执行。
        // 通常，withContext 用于在 Kotlin 协程中改变代码的上下文，但是 flow {...} 构建器中的代码必须遵循上下文保存属性，并且不允许从其他上下文中发射（emit）。
        // 会抛出异常java.lang.IllegalStateException: Flow invariant is violated，留在执行过程中是不能切换线程的(官网文章中都以上下文替代)
        //如果需要切换上下文森，需要用flowOn操作符
        fun testWithContextFlow() {
            fun simple(): Flow<Int> = flow {
                // 在流构建器中更改消耗 CPU 代码的上下文的错误方式
                withContext(Dispatchers.Default) {
                    for (i in 1..3) {
                        Thread.sleep(100) // 假装我们以消耗 CPU 的方式进行计算
                        emit(i) // 发射下一个值
                    }
                }
            }

            fun main() = runBlocking<Unit> {
                simple().collect { value -> log(value) }
            }
            main()
        }

        //flowOn操作符 切换上下文
        //通过日志会发现，发送是在后台线程，收集发生在主线程
        //CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , Emitting 1
        //CoroutinesLog: thread-name:main , Collected 1
        //CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , Emitting 2
        //CoroutinesLog: thread-name:main , Collected 2
        //CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , Emitting 3
        //CoroutinesLog: thread-name:main , Collected 3
        fun testChangeThread() {
            fun simple(): Flow<Int> = flow {
                for (i in 1..3) {
                    Thread.sleep(100) // 假装我们以消耗 CPU 的方式进行计算
                    log("Emitting $i")
                    emit(i) // 发射下一个值
                }
            }.flowOn(Dispatchers.Default) // 在流构建器中改变消耗 CPU 代码上下文的正确方式
            //这里要观察的另一件事是 flowOn 操作符已改变流的默认顺序性。
            // 现在收集发生在一个协程中（“coroutine#1”）而发射发生在运行于另一个线程中与收集协程并发运行的另一个协程（“coroutine#2”）中。
            // 当上游流必须改变其上下文中的 CoroutineDispatcher 的时候，flowOn 操作符创建了另一个协程。

            fun main() = runBlocking<Unit> {
                simple().collect { value ->
                    log("Collected $value")
                }
            }
            main()
        }

        //异常捕获分为收集侧和发送侧
        fun testCatchFlow() {

            fun simple(): Flow<String> =
                flow {
                    for (i in 1..3) {
                        println("Emitting $i")
                        emit(i) // 发射下一个值
                    }
                }
                    .map { value ->
                        check(value <= 1) { "Crashed on $value" }
                        "string $value"
                    }

            // 发送侧的捕获，直接try..catch就行
            fun main() = runBlocking<Unit> {
                try {
                    //这里的catch捕获是catch关键字上游的异常，下游异常不会捕获
                    simple().catch { e -> emit("Caught $e") }.collect { value -> println(value) }
                } catch (e: Throwable) {
                    println("Caught $e")
                }

                //使用这种声明式写法就可以捕获发射端所有异常了
                //其实就是先用onEach声明
                simple()
                    .onEach { value ->
                        check(value.length <= 4) { "Collected $value" }
                        println(value)
                    }
                    .catch { e -> println("Caught $e") }
                    .collect()
            }

            main()
        }

        // 监听流的完成情况
        fun testFlowCompletely(){
            fun simple(): Flow<Int> = (1..3).asFlow()

            fun main() = runBlocking<Unit> {
                try {
                    simple().collect { value -> println(value) }
                } finally { //第一种方式用finally
                    println("Done")
                }

                simple()
                    .onCompletion { println("Done") } //第二种写法onCompletion
                    .collect { value -> println(value) }
            }
        }
    }

}