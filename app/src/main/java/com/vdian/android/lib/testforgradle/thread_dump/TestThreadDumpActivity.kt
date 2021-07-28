package com.vdian.android.lib.testforgradle.thread_dump

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vdian.android.lib.testforgradle.R

class TestThreadDumpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_thread2)
        ThreadDumpHelper().dumpThreadInfo();
    }
}