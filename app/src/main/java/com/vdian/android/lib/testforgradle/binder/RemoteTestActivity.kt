package com.vdian.android.lib.testforgradle.binder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.R

class RemoteTestActivity : AppCompatActivity() {

    private var mRemoteManager: ICompute? = null
    private var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mRemoteManager = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            android.util.Log.i("remoteTest", "test binder is onServiceConnected")
            mRemoteManager = ICompute.Stub.asInterface(service)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_test)
    }

    fun bindBinder(v: View) {
        bindService()
    }

    fun testBinder(v: View) {
        var result = mRemoteManager?.add(1, 2)
        android.util.Log.i("remoteTest", "test binder is $result")
    }

    /**
     * 绑定服务按钮的点击事件
     *
     * @param view 视图
     */
    fun bindService() {
        val intent = Intent(this, IComputerService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

}