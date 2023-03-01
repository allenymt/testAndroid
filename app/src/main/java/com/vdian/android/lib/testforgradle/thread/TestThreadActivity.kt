package com.vdian.android.lib.testforgradle.thread

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.thread.cas.CasTest

class TestThreadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_thread)
    }

    fun printABC(v: View) {
        // 测试哪个放开哪个
//        var test =  WaitTest()
//        test.main(null)
//        YieldTest.main(null)
//        var producer =  Producer()
//        producer.main()
//
//        var producerNew =  ProducerNew()
//        producerNew.main()
//
//        MutiThreadCondition.main(null)

        // 多线程顺序打印 ABC
//        val testPrintAbc = TestPrintAbc()
//        testPrintAbc.main()

        var cas = CasTest()
        cas.main()
    }
}