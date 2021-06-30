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
//        WaitTest test =  new WaitTest();
//        test.main(args);
//        YieldTest.main(args);
//        Producer producer = new Producer();
//        producer.main();

//        ProducerNew producerNew = new ProducerNew();
//        producerNew.main();

//        MutiThreadCondition.main(null);
        val testPrintAbc = TestPrintAbc()
        testPrintAbc.main()
    }
}