package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.*

/**
 * @author yulun
 * @since 2022-02-11 14:45
 * 调试协程与线程
 * 协程可以在一个线程上挂起并在其它线程上恢复。 如果没有特殊工具，甚至对于一个单线程的调度器也是难以弄清楚协程在何时何地正在做什么事情。
 */
class TestDebugCoroutine {

    //当一个协程被其它协程在 CoroutineScope 中启动的时候， 它将通过 CoroutineScope.coroutineContext 来承袭上下文，
    // 并且这个新协程的 Job 将会成为父协程作业的 子 作业。当一个父协程被取消的时候，所有它的子协程也会被递归的取消。
    //
    //然而，当使用 GlobalScope 来启动一个协程时，则新协程的作业没有父作业。 因此它与这个启动的作用域无关且独立运作。


    //父协程的职责
    //一个父协程总是等待所有的子协程执行结束。父协程并不显式的跟踪所有子协程的启动，并且不必使用 Job.join 在最后的时候等待它们：
    object Test {

        //线程局部数据
        //有时，能够将一些线程局部数据传递到协程与协程之间是很方便的。 然而，由于它们不受任何特定线程的约束，如果手动完成，
        // 可能会导致出现样板代码。
        //
        //ThreadLocal， asContextElement 扩展函数在这里会充当救兵。它创建了额外的上下文元素，
        // 且保留给定 ThreadLocal 的值，并在每次协程切换其上下文时恢复它。
        val threadLocal = ThreadLocal<String?>() // declare thread-local variable
        fun testThreadRef() {
            //其实还是借助了threadLocal,第二步和第三步可以看到，线程切换了后值并没有改变
            //CoroutinesLog: thread-name:main , Pre-main, current thread: Thread[main,5,main], thread local value: 'main'
            //CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , Launch start, current thread: Thread[DefaultDispatcher-worker-1,5,main], thread local value: 'launch'
            //CoroutinesLog: thread-name:DefaultDispatcher-worker-3 , After yield, current thread: Thread[DefaultDispatcher-worker-1,5,main], thread local value: 'launch'
            //CoroutinesLog: thread-name:main , Post-main, current thread: Thread[main,5,main], thread local value: 'main'
            fun main() = runBlocking<Unit> {
                threadLocal.set("main")
                log("Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
                val job =
                    launch(Dispatchers.IO + threadLocal.asContextElement(value = "launch")) {
//                        threadLocal.set("xx1")
                        log("Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
                        delay(1000)
                        yield()
//                        threadLocal.set("xx2")
                        log("After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
                    }
                job.join()
                log("Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
            }
            main()
        }
    }


    //todo 关于协程 在线程之间的值传递，更多的要参考 ThreadContextElement
}