package com.vdian.android.lib.testforgradle.ktl.coroutines

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * @author yulun
 * @since  17:41
 * 自定义协程作用域，注意GlobalScope和
 */
class TestCoroutineScope {
    object Test {
        // 这个输出很有意思，之前的理解有偏差
        // 18:41:49.600  CoroutinesLog: thread-name:main , do TestCoroutinesActivity onCreate
        // 18:41:49.600  CoroutinesLog: thread-name:main , Do runBlocking 开始执行blocking，阻塞了主线程
        // 18:41:49.615  CoroutinesLog: thread-name:DefaultDispatcher-worker-3 , Task from runBlocking1 start
        // 18:41:49.617  CoroutinesLog: thread-name:DefaultDispatcher-worker-2 , Task from runBlocking2 start
        // 18:41:49.617  CoroutinesLog: thread-name:DefaultDispatcher-worker-1 , Coroutine scope is over
        // 18:41:50.120  CoroutinesLog: thread-name:DefaultDispatcher-worker-3 , Task from runBlocking2 end
        // 18:41:50.622  CoroutinesLog: thread-name:DefaultDispatcher-worker-3 , Task from runBlocking1 end 这里block执行完，后面的代码才执行
        // 18:41:50.623  CoroutinesLog: thread-name:main , Do GlobalScope  start 开始执行后面的代码
        // 18:41:50.627  CoroutinesLog: thread-name:main , Do GlobalScope  end
        // 18:41:50.633  CoroutinesLog: thread-name:main , do TestCoroutinesActivity start  此时才执行ac的start
        // 18:41:50.679  CoroutinesLog: thread-name:main , Do coroutineScope1  start coroutineScope1和coroutineScope2的同步开始的
        // 18:41:50.680  CoroutinesLog: thread-name:main , Do coroutineScope2  start
        // 18:41:50.681  CoroutinesLog: thread-name:main , Task from coroutine2 scope
        // 18:41:50.695  CoroutinesLog: thread-name:main , Task from nested launch1 start
        // 18:41:50.695  CoroutinesLog: thread-name:main , Task from nested launch2 start
        // 18:41:50.696  CoroutinesLog: thread-name:main , Task from coroutineScope2 start
        // 18:41:50.696  CoroutinesLog: thread-name:main , Task from coroutine scope
        // 18:41:50.706  CoroutinesLog: thread-name:main , Task from coroutineScope2 end
        // 18:41:50.706  CoroutinesLog: thread-name:main , Do coroutineScope2  end 由于coroutineScope2等待时间段，所以他先执行结束
        // 18:41:50.799  CoroutinesLog: thread-name:main , Task from nested launch1 end
        // 18:41:51.198  CoroutinesLog: thread-name:main , Task from nested launch2 end
        // 18:41:51.199  CoroutinesLog: thread-name:main , Do coroutineScope1  end coroutineScope1后面才结束，所以coroutineScope不会阻塞，而是挂起让其他任务执行
        fun testCoroutineScope() {
            log("Do runBlocking")
            //runBlocking是指阻塞调用它的线程,在这里是主线程调用的，所以阻塞的是主线程，从ac的start可以看出来，而且阻塞后是runBlocking后面的代码都不执行，直到runBlocking执行完才执行
            runBlocking(Dispatchers.IO) {
                launch {
                    log("Task from runBlocking1 start")
                    delay(1000)
                    log("Task from runBlocking1 end")
                }
                launch {
                    log("Task from runBlocking2 start")
                    delay(500)
                    log("Task from runBlocking2 end")
                }
                log("Coroutine scope is over")
            }
            log("Do GlobalScope  start")
            GlobalScope.launch(Dispatchers.Main) {
                log("Do coroutineScope1  start")
                // 这里会suspend，不会阻塞
                coroutineScope {
                    launch {
                        log("Task from nested launch1 start")
                        delay(100)
                        log("Task from nested launch1 end")
                    }
                    launch {
                        log("Task from nested launch2 start ")
                        delay(500)
                        log("Task from nested launch2 end")
                    }
                    delay(10)
                    log("Task from coroutine scope")
                }
                log("Do coroutineScope1  end")
            }


            GlobalScope.launch(Dispatchers.Main) {
                log("Do coroutineScope2  start")
                // 当coroutineScope1挂起后，由于这里的delay时间更少，所以先执行了coroutineScope2，相当于一个队列，在挂起函数钱都是同步跑的，把任务一个个放进去，然后根据执行时间依次执行
                coroutineScope {
                    launch {
                        log("Task from coroutineScope2 start")
                        delay(10)
                        log("Task from coroutineScope2 end")
                    }
                    log("Task from coroutine2 scope")
                }
                log("Do coroutineScope2  end")
            }
            log("Do GlobalScope  end")
        }

        // TODO 感觉无大用
        // supervisorScope 函数用于创建一个使用了 SupervisorJob 的 coroutineScope，该作用域的特点就是抛出的异常不会连锁取消同级协程和父协程
        // 但从实际的结果来看，这个不对，抛异常后APP直接退出了
        fun testSupervisorScope() {
            try {
                runBlocking {
                    launch {
                        delay(100)
                        log("Task from runBlocking")
                    }
                    supervisorScope {
                        launch {
                            delay(500)
                            log("Task throw Exception")
                            try {
                            } catch (e: Exception) {
                                throw Exception("failed")
                            }
                        }
                        launch {
                            delay(600)
                            log("Task from nested launch")
                        }
                    }
                    log("Coroutine scope is over")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        private val mainScope = MainScope()
        fun testSelfScope() {
            mainScope.launch {
                repeat(5) {
                    delay(1000L * it)
                }
            }
            mainScope.cancel()
        }

        fun testCoroutineDelegation() {
            runBlocking {
                val activity = TestCoroutineDelegation(CoroutineScope(Dispatchers.Default))
                activity.onCreate()
                delay(1000)
                activity.onDestroy()
                delay(1000)
            }
        }
    }
}
//假设我们在 Activity 中先后启动了多个协程用于执行异步耗时操作，
// 那么当 Activity 退出时，必须取消所有协程以避免内存泄漏。我们可以通过保留每一个 Job 引用然后在 onDestroy方法里来手动取消，
// 但这种方式相当来说会比较繁琐和低效。kotlinx.coroutines 提供了 CoroutineScope 来管理多个协程的生命周期
//我们可以通过创建与 Activity 生命周期相关联的协程作用域来管理协程的生命周期。
// CoroutineScope 的实例可以通过 CoroutineScope() 或 MainScope() 的工厂函数来构建。前者创建通用作用域，
// 后者创建 UI 应用程序的作用域并使用 Dispatchers.Main 作为默认的调度器
// 已取消的作用域无法再创建协程。因此，仅当控制其生命周期的类被销毁时，才应调用 scope.cancel()。例如，使用 viewModelScope 时， ViewModel 会在自身的 onCleared() 方法中自动取消作用域
// 之前学的委托正常写法,第一种写法就是把委托的对象实例交给外部
class TestCoroutineDelegation(c:CoroutineScope) : CoroutineScope by c {
    fun onCreate() {
        launch {
            repeat(5) {
                delay(200L * it)
                log(it)
            }
        }
        log("Activity Created")
    }

    fun onDestroy() {
        cancel()
        log("Activity Destroyed")
    }
}

// 新的委托写法，委托本质上就是持有一个被委托类的实例，第二种写法是在类这里默认创建了委托的对象，从反编译的结果也能看出来，第一种写法多了个构造函数，第二种写法变量直接初始化
class TestCoroutineDelegation2 : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    fun onCreate() {
        launch {
            repeat(5) {
                delay(200L * it)
                log(it)
            }
        }
        log("Activity Created")
    }

    fun onDestroy() {
        cancel()
        log("Activity Destroyed")
    }

}
