package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.*
import kotlin.concurrent.thread

/**
 * @author yulun
 * @since 2022-01-26 15:02
 */
class TestCoroutinesBasics {
    object Test{
        fun main() {
            //段落1
            GlobalScope.launch { // 在后台启动一个新的协程并继续
                delay(1000L) // 非阻塞的等待 1 秒钟（默认时间单位是毫秒）
                println("World!") // 在延迟后打印输出
            }

            //段落2
            thread{ // 在后台启动一个新的协程并继续
                Thread.sleep(1000L) // 非阻塞的等待 1 秒钟（默认时间单位是毫秒）
                println("World!") // 在延迟后打印输出
            }

            //段落1和段落2具有相同的效果
            println("Hello,") // 协程已在等待时主线程还在继续
            Thread.sleep(2000L) // 阻塞主线程 2 秒钟来保证 JVM 存活,协程本身不会阻断jvm的运行
        }

        // 阻塞
        // 此段函数执行后的结果是Hello,World!
        fun b(){
            GlobalScope.launch { // 在后台启动一个新的协程并继续
                delay(1000L)
                log("World!")
            }
            log("Hello,") // 主线程中的代码会立即执行
            runBlocking {     // 但是这个表达式阻塞了主线程
                delay(500L)  // ……我们延迟 2 秒来保证 JVM 的存活,其实在这个测试case里，对于当前进程的jvm一直存活
            }
        }

        //以下代码中 runBlocking 会早于 GlobalScope 输出日志
        //runBlocking 本身带有阻塞线程的意味，但其内部运行的协程又是非阻塞的，读者需要明白这两者的区别
        //看下输出结果
        //2022-02-07 16:18:39.029 25387-25387 CoroutinesLog: thread-name:main , runBlocking start 第一步，GlobalScope start也有可能先输出的，要看io线程的启动速度和cpu轮转情况
        //2022-02-07 16:18:39.030 25387-25450 CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , GlobalScope start 因为只延迟了100ms，可以看到GlobalScope先输出完成
        //2022-02-07 16:18:39.131 25387-25450 CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , GlobalScope end
        //2022-02-07 16:18:39.530 25387-25387 CoroutinesLog: thread-name:main , runBlocking end ，会阻塞当前线程后续代码的执行
        //2022-02-07 16:18:39.530 25387-25387 CoroutinesLog: thread-name:main , before sleep 主线程代码块的执行，没什么好说的
        //2022-02-07 16:18:39.730 25387-25387 CoroutinesLog: thread-name:main , after sleep
        fun runBlocking1() {
            GlobalScope.launch(Dispatchers.IO) {
                log("GlobalScope start")
                delay(100)
                log("GlobalScope end")
            }
            runBlocking {
                log("runBlocking start")
                delay(500)
                log("runBlocking end")
            }
            log("before sleep")
            //主动休眠两百毫秒，使得和 runBlocking 加起来的延迟时间多于六百毫秒
            Thread.sleep(200)
            log("after sleep")
        }

        //runBlocking 本身带有阻塞线程的意味，但其内部运行的协程又是非阻塞的，读者需要明白这两者的区别
        //runBlocking的阻塞是指在他内部使用它的scope运行的协程运行时才阻塞，可以看到GlobalScope运行是不阻塞的
        //450  CoroutinesLog: thread-name:main , start
        //569  CoroutinesLog: thread-name:main , launchA - 0
        //570  CoroutinesLog: thread-name:main , launchB - 0
        //570  CoroutinesLog: thread-name:main , launchC - 0
        //590  CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , GlobalScope - 0
        //670  CoroutinesLog: thread-name:main , launchA - 1
        //671  CoroutinesLog: thread-name:main , launchB - 1
        //671  CoroutinesLog: thread-name:main , launchC - 1
        //710  CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , GlobalScope - 1
        //770  CoroutinesLog: thread-name:main , launchA - 2
        //771  CoroutinesLog: thread-name:main , launchB - 2
        //771  CoroutinesLog: thread-name:main , launchC - 2
        //771  CoroutinesLog: thread-name:main , end
        //831  CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , GlobalScope - 2 GlobalScope运行是不阻塞的
        fun runBlocking2() {
            log("start")
            runBlocking {
                launch {
                    repeat(3) {
                        delay(100)
                        log("launchA - $it")
                    }
                }
                launch {
                    repeat(3) {
                        delay(100)
                        log("launchB - $it")
                    }
                }
                GlobalScope.launch {
                    repeat(3) {
                        delay(120)
                        log("GlobalScope - $it")
                    }
                }
                launch {
                    repeat(3) {
                        delay(100)
                        log("launchC - $it")
                    }
                }
            }
            log("end")
        }
    }

}