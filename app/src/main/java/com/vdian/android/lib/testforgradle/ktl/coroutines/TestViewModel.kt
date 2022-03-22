package com.vdian.android.lib.testforgradle.ktl.coroutines

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.*

/**
 * @author yulun
 * @since 2022-02-10 17:45
 *
 */
class TestViewModel : ViewModel() {
    // 在某些情况下，我们需要先完成特定的异步计算任务，根据计算结果来向 LiveData 回调值，
    // 此时就可以使用 LiveData ktx 提供的 liveData 构建器函数来执行 suspend 函数所代表的异步计算任务（耗时任务），并将结果赋值给 LiveData
    val user: LiveData<String> = liveData {
        log("TestViewModel LiveData start")
        val data = get("https://developer.android.com")
        emit(data)
        log("TestViewModel LiveData end")
    }
    fun fetchDocs() {
        viewModelScope.launch {
            val result = get("https://developer.android.com")
            log("TestViewModel $result")
        }

        suspend {

        }.createCoroutine(object :Continuation<Unit>{
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                TODO("Not yet implemented")
            }
        }).resumeWith(Result.success(Unit))

        suspend {  }.startCoroutine(object : Continuation<Unit>{
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                TODO("Not yet implemented")
            }
        })
    }

    suspend fun get(url: String) = withContext(Dispatchers.IO) { url }

}