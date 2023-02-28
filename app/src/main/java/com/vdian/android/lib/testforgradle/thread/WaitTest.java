package com.vdian.android.lib.testforgradle.thread;

/**
 * @author yulun
 * @sinice 2020-03-14 14:04
 *
 * wait(0)： 调用者必须持有这个锁，调用者自己切换到等待阻塞状态，
 * 并释放锁，其他处于就绪态的线程会来竞争这个锁(如果是wait过的，那是等待阻塞态，不会来竞争，
 * 这时先要notify，再wait)。直到其他持有相同锁的线程调用notify/notifyAll，让这个线程重新进入就绪态
 *
 * sleep:让当前线程休眠，即当前线程会从“运行状态”进入到“休眠(阻塞)状态”。
 * sleep()会指定休眠时间，线程休眠的时间会大于/等于该休眠时间；在线程重新被唤醒时，
 * 它会由“阻塞状态”变成“就绪状态”，从而等待cpu的调度执行。和wait的区别是不会释放锁
 */
class ThreadA extends Thread{

    public ThreadA(String name) {
        super(name);
    }

    public void run() {
        System.out.println(Thread.currentThread().getName()+" start");
        synchronized (this) {
//            while (true){
//
//            }
            // 唤醒当前的wait线程
            try{
                // 注意 sleep不会释放锁，因此尽快主线程只等待了wait了3s, 但这里sleep了10s, 因此要等这里执行完后，释放了锁才会释放
                Thread.sleep(10000);
//                wait();
            }catch (Exception e){
                e.printStackTrace();
            }

            // 当外面是wait(time > 0 )的时候其实不需要
            // 当入参为空的时候，就需要了，wait(0)和无线等待是同价的
            // 这里其实执不执行nofify都不重要了
            notify();
            System.out.println(Thread.currentThread().getName()+" call notify()");
        }
    }
}

public class WaitTest {
    volatile  int bb =1;
    volatile ThreadA cc;
    public void main(String[] args) {


        //2023-02-28 17:25:06.823 29332-29332/com.vdian.android.lib.testforgradle I/System.out: main start t1
        //2023-02-28 17:25:06.824 29332-29332/com.vdian.android.lib.testforgradle I/System.out: main wait()
        //2023-02-28 17:25:06.824 29332-29584/com.vdian.android.lib.testforgradle I/System.out: t1 start
        //2023-02-28 17:25:16.825 29332-29332/com.vdian.android.lib.testforgradle I/System.out: main continue
        ThreadA t1 = new ThreadA("t1");
        synchronized(t1) {
            try {
                // 启动“线程t1”
                System.out.println(Thread.currentThread().getName()+" start t1");
                t1.start();
                // 主线程等待t1通过notify()唤醒。
                System.out.println(Thread.currentThread().getName()+" wait()");
                // 注意这里必须要用锁的持有者去调用，调用后阻塞的事当前线程
                // 带有时间值的，在时间到了后，当前调用者会自动切换到就绪态
                t1.wait(3000);//wait 必须是当前拥有锁的对象去调用

                // 当入参为0 或者不传，竟然是一直阻塞态，相当于无限等待
//                t1.wait();
                System.out.println(Thread.currentThread().getName()+" continue");
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName()+" exception");
                e.printStackTrace();
            }
        }
    }
}
