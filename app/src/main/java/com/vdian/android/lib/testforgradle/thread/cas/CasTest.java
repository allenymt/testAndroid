package com.vdian.android.lib.testforgradle.thread.cas;

import java.util.concurrent.CountDownLatch;

/**
 * @author yulun
 * @since 2023-02-28 18:43
 * 模拟100个线程同时操作某一个变量，然后用cas乐观锁来解决试试看
 */
public class CasTest {
    private int count = 0;

    // 乐观锁 cas
    public void testPessimistic() {

        int currentCount = count;
        while (!compareAndSet(currentCount, currentCount + 1)) {

        }
    }

    // 模拟cas的逻辑
    private synchronized boolean compareAndSet(int currentCount, int expectCount) {
        if (count == currentCount) {
            count = expectCount;
            return true;
        }
        return false;
    }

    // 悲观锁
    public synchronized void testOptimistic() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        increment();
    }

    private void increment() {
        count++;
    }

    //cas真的快很多
    //I/System.out: 悲观锁耗时=580 , count = 100
    //I/System.out: 乐观锁耗时=37 , count = 100
    public void main() {
        int size = 100;
        long startTime1 = System.currentTimeMillis();
        CountDownLatch downLatch = new CountDownLatch(size);
        // 先测试悲观
        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    testOptimistic();
                    downLatch.countDown();
                }
            });
            t.start();
        }
        try {
            downLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime1 = System.currentTimeMillis();
        System.out.println("悲观锁耗时=" + (endTime1 - startTime1) + " , count = " + count);

        CountDownLatch downLatch1 = new CountDownLatch(size);
        count = 0;
        // 先测试悲观
        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    testPessimistic();
                    downLatch1.countDown();
                }
            });
            t.start();
        }
        try {
            downLatch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime2 = System.currentTimeMillis();
        System.out.println("乐观锁耗时=" + (endTime2 - endTime1) + " , count = " + count);
    }
}
