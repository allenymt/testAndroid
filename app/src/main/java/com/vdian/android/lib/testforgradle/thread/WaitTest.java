package com.vdian.android.lib.testforgradle.thread;

/**
 * @author yulun
 * @sinice 2020-03-14 14:04
 */
class ThreadA extends Thread{

    public ThreadA(String name) {
        super(name);
    }

    public void run() {
        synchronized (this) {
            System.out.println(Thread.currentThread().getName()+" call notify()");
            // 唤醒当前的wait线程
            try{
                wait();
            }catch (Exception e){
                e.printStackTrace();
            }

//            notify();
        }
    }
}

public class WaitTest {
    volatile  int bb =1;
    volatile ThreadA cc;
    public void main(String[] args) {


        ThreadA t1 = new ThreadA("t1");
        synchronized(t1) {
            try {
                // 启动“线程t1”
                System.out.println(Thread.currentThread().getName()+" start t1");
                t1.start();

//                Thread.sleep(3000);
                // 主线程等待t1通过notify()唤醒。
                System.out.println(Thread.currentThread().getName()+" wait()");
                t1.wait(3000);//wait 必须是当前拥有锁的对象去调用
                System.out.println(Thread.currentThread().getName()+" continue");
//                notify();
                t1.notify();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName()+" exception");
                e.printStackTrace();
            }
        }
    }
}
