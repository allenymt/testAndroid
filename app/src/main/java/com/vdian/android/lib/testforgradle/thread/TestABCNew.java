package com.vdian.android.lib.testforgradle.thread;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author yulun
 * @sinice 2020-03-29 18:57
 */
public class TestABCNew {

    private static Runnable getThreadA(final ConditionService service) {
        return new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<2;i++) {
                    service.excuteA();
                }
            }
        };
    }

    private static Runnable getThreadB(final ConditionService service) {
        return new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<2;i++) {
                    service.excuteB();
                }
            }
        };
    }

    private static Runnable getThreadC(final ConditionService service) {
        return new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<2;i++) {
                    service.excuteC();
                }
            }
        };
    }

    public static void main(String[] args) throws InterruptedException{
        ConditionService service = new ConditionService();
        Runnable A = getThreadA(service);
        Runnable B = getThreadB(service);
        Runnable C = getThreadC(service);

        new Thread(B, "B").start();
        new Thread(C, "C").start();
        Thread.sleep(100);
        new Thread(A, "A").start();
    }
}


 class ConditionService {

    // 通过nextThread控制下一个执行的线程
    private static int nextThread = 1;
    private ReentrantLock lock = new ReentrantLock();
    // 有三个线程，所以注册三个Condition
    Condition conditionA = lock.newCondition();
    Condition conditionB = lock.newCondition();
    Condition conditionC = lock.newCondition();

    public void excuteA() {
        try {
            System.out.println("excuteA lock");
            lock.lock();
            while (nextThread != 1) {
                System.out.println("excuteA await");
                conditionA.await();
            }
            System.out.println(Thread.currentThread().getName() + " 工作");
            nextThread = 2;
            conditionB.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("excuteA unlock");
            lock.unlock();
        }
    }

    public void excuteB() {
        try {
            System.out.println("excuteB lock");
            lock.lock();
            while (nextThread != 2) {
                System.out.println("excuteB await");
                conditionB.await();
            }
            System.out.println(Thread.currentThread().getName() + " 工作");
            nextThread = 3;
            conditionC.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("excuteB unlock");
            lock.unlock();
        }
    }

    public void excuteC() {
        try {
            System.out.println("excuteC lock");
            lock.lock();
            while (nextThread != 3) {
                System.out.println("excuteC await");
                conditionC.await();
            }
            System.out.println(Thread.currentThread().getName() + " 工作");
            nextThread = 1;
            conditionA.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("excuteC unlock");
            lock.unlock();
        }
    }
}