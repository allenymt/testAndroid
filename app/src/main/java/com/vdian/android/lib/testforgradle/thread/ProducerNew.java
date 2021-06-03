package com.vdian.android.lib.testforgradle.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yulun
 * @sinice 2020-03-15 09:10
 */
public class ProducerNew {
    private ReentrantLock lock = new ReentrantLock();
    private Condition fullContition = lock.newCondition();
//    private Condition emptyContition = lock.newCondition();
    private List<String> itemList = new ArrayList<>();

    public void main() {
                    ThreadProducerNew threadProducer = new ThreadProducerNew();

        ThreadCustomerNew threadCustomer = new ThreadCustomerNew();
        threadProducer.start();
        threadCustomer.start();
    }

    class ThreadProducerNew extends Thread {

        public ThreadProducerNew() {
            super("producer");
        }

        @Override
        public void run() {

            while (true) {
                try {
                    System.out.println("producernew item lock");
                    lock.lock();
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        itemList.add("item" + i);
                        System.out.println("producernew item " + itemList.get(i));
                    }
                    //通知仓库满了，唤起消费线程
                    fullContition.signal();//会释放锁吗？调用这个后，消费线程直接被唤起了
                    System.out.println("producernew after signal");
                    try {
                        //生成线程阻塞
//                        fullContition.await();
                        System.out.println("producernew after await");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("producernew item Exception");
                } finally {
                    System.out.println("producernew item unlock");
                    lock.unlock();
                }
            }
        }
    }

    class ThreadCustomerNew extends Thread {

        public ThreadCustomerNew() {
            super("customer");
        }

        @Override
        public void run() {

            while (true) {
                try {
                    System.out.println("customNew item lock");
                    //获取锁
                    lock.lock();//获取不到锁就阻塞了，此线程会休眠等待，当condition调用signal的时候，就被唤醒了
                    for (int i = 0; i < itemList.size(); i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("customNew item " + itemList.get(i));
                    }
                    itemList.clear();
                    fullContition.signal();
                    try {
//                        fullContition.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("customNew item afterAwait");
                } catch (Exception e) {
                    System.out.println("customNew item Exception");
                } finally {
                    System.out.println("customNew item unlock");
                    lock.unlock();
                    try {
                        Thread.sleep(100);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }

        }
    }
}
