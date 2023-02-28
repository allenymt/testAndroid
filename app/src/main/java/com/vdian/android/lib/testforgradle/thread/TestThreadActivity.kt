package com.vdian.android.lib.testforgradle.thread

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.vdian.android.lib.testforgradle.R

class TestThreadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_thread)
    }

    fun printABC(v: View){
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

        val testPrintAbc = TestPrintAbc()
        testPrintAbc.main()
    }
}