package com.vdian.android.lib.testforgradle.thread;

/**
 * @author yulun
 * @sinice 2020-03-14 14:08
 */
public class TestThreadMain {
    public static void main(String[] args){
//        WaitTest test =  new WaitTest();
//        test.main(args);
//        YieldTest.main(args);
//        Producer producer = new Producer();
//        producer.main();

//        ProducerNew producerNew = new ProducerNew();
//        producerNew.main();

//        MutiThreadCondition.main(null);

        TestPrintAbc testPrintAbc= new TestPrintAbc();
        testPrintAbc.main();
    }
}
