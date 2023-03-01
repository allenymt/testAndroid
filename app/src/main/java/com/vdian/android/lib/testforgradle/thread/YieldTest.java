package com.vdian.android.lib.testforgradle.thread;

/**
 * @author yulun
 * @sinice 2020-03-14 14:45
 * wait是调用后到了阻塞等待态，yield是就绪态
 * 它能让当前线程由“运行状态”进入到“就绪状态”，从而让其它具有相同优先级的等待线程获取执行权；
 * 但是，并不能保证在当前线程调用yield()之后，其它具有相同优先级的线程就一定能获得执行权；
 * 也有可能是当前线程又进入到“运行状态”继续运行！不需要锁，竞争完全是随机的。
 */
// YieldTest.java的源码
class ThreadB extends Thread {
    public ThreadB(String name) {
        super(name);
    }

    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.printf("%s [%d]:%d\n", this.getName(), this.getPriority(), i);
            // i整除4时，调用yield
            if (i % 4 == 0)
                Thread.yield();
        }
    }
}

public class YieldTest {
    public static void main(String[] args) {
        ThreadB t1 = new ThreadB("t1");
        ThreadB t2 = new ThreadB("t2");
        t1.start();
        t2.start();
    }
}
