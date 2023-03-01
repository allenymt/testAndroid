package com.vdian.android.lib.testforgradle.workmanager

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivityWorkManagerTestBinding
import java.util.concurrent.TimeUnit

class WorkManagerTestActivity : AppCompatActivity() {
    private var _wmTViewBinding: ActivityWorkManagerTestBinding? = null

    private val wmTViewBinding get() = _wmTViewBinding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在使用ViewBinding或者DataBinding的时候，注意下setContentView()的位置和入参，setContentView(R.layout.xxx) 不能再用了
        // 因为ViewBinding绑定的实例和setContentView不是同一个
        _wmTViewBinding = ActivityWorkManagerTestBinding.inflate(layoutInflater)
        setContentView(_wmTViewBinding?.root)


        //执行正常任务
        wmTViewBinding.normal.setOnClickListener {
            val inputData = Data.Builder().putBoolean("isStart", true)
                .putInt("delay", 200).build()
            val requestOneTime = OneTimeWorkRequest.Builder(CustomWorker::class.java)
                .setInputData(inputData)
                .build()

            //订阅获取回传的数据
            WorkManager.getInstance(applicationContext)
                // requestId is the WorkRequest id
                .getWorkInfoByIdLiveData(requestOneTime.id)
                .observe({ lifecycle }, {
                    if (it.state.isFinished) {
                        it.outputData.getString("doWork")?.let { it1 -> android.util.Log.d("WorkManagerTest", it1) }
                    }
                })

            //用WorkManager启动任务
            WorkManager.getInstance(applicationContext).enqueue(requestOneTime)



            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) //在网络连接的情况下
                .setRequiresDeviceIdle(true) //设备待机空闲
                .setRequiresBatteryNotLow(true) //设备不处在低电量的情况
                .setRequiresStorageNotLow(true) //内存不紧张的情况下
                .setRequiresCharging(true) //充电状态
                .build()

            //20是重复间隔时间
            val requestPeriodic = PeriodicWorkRequest.Builder(CustomWorker::class.java, 20, TimeUnit.MINUTES)
                .addTag("CustomWorker")
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()
            //用WorkManager启动任务
            WorkManager.getInstance(applicationContext).enqueue(requestPeriodic)
            wmTViewBinding.root.postDelayed({
                android.util.Log.e("WorkManagerTest"," cancel requestPeriodic id is ${requestPeriodic.id}")
                WorkManager.getInstance(applicationContext).cancelWorkById(requestPeriodic.id)
            }, 5000)
        }

        val requestA = OneTimeWorkRequest.Builder(AWorker::class.java)
            .build()

        val requestB = OneTimeWorkRequest.Builder(BWorker::class.java)
            .build()

        val requestC = OneTimeWorkRequest.Builder(CWorker::class.java)
            .build()

        val requestD = OneTimeWorkRequest.Builder(DWorker::class.java)
            .build()

        val requestE = OneTimeWorkRequest.Builder(EWorker::class.java)
            .build()

        // 执行串行任务
        wmTViewBinding.serial.setOnClickListener {


            WorkManager.getInstance(applicationContext)
                .beginWith(requestA)
                .then(requestB)
                .then(requestC)
                .then(requestD)
                .enqueue()

        }

        // 执行并行任务
        wmTViewBinding.parallel.setOnClickListener {
            WorkManager.getInstance(applicationContext)
                .beginWith(listOf(requestA,requestB,requestC,requestD))
                .enqueue()

        }

        // 执行多路径任务
        wmTViewBinding.multi.setOnClickListener {
            WorkManager.getInstance(applicationContext)
                .beginWith(listOf(requestA, requestB))
                .then(requestC)
                .then(listOf(requestD))
                .enqueue()

            val chain1 = WorkManager.getInstance(applicationContext)
                .beginWith(requestA)
                .then(requestB)

            val chain2 = WorkManager.getInstance(applicationContext)
                .beginWith(requestC)
                .then(requestD)
            WorkContinuation.combine(listOf(chain1, chain2))
                .then(requestE)
                .enqueue()
        }
    }
}


class CustomWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {

        android.util.Log.d("WorkManagerTest", "当前Worker执行的线程 ${Thread.currentThread()}")
        //这个是构建请求传进来的数据，在
        val delayTime = inputData.getLong("delay", 0)
        val isStart = inputData.getBoolean("isStart", false)
        android.util. Log.d("WorkManagerTest","$delayTime")
        Thread.sleep(delayTime)
        //任务完成发送回调数据，在WorkRequest中体现
        val outputData = Data.Builder().putString("doWork", "任务已执行完").build()
        if (isStart) {
            return Result.success(outputData)
        }
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        //任务结束后会回调此方法
        android.util.Log.d("WorkManagerTest", "onStopped")
    }
}



class AWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Thread.sleep(200)
        android.util.Log.d("WorkManagerTest-AWorker","A执行了")
        return Result.success()
    }
}

class BWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Thread.sleep(200)
        android.util.Log.d("WorkManagerTest-BWorker","B执行了")
        return Result.success()
    }
}

class CWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Thread.sleep(200)
        android.util.Log.d("WorkManagerTest-CWorker","C执行了")
        return Result.success()
    }
}

class DWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Thread.sleep(200)
        android.util.Log.d("WorkManagerTest-DWorker","D执行了")
        return Result.success()
    }
}


class EWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Thread.sleep(200)
        android.util.Log.d("WorkManagerTest-EWorker","E执行了")
        return Result.success()
    }
}


