package com.vdian.android.lib.testforgradle.thread;

/**
 * @author yulun
 * @sinice 2020-03-18 11:31
 */
public class TestPrintAbc {
    // 总的控制变量，加volatile是为了保证线程之间的可见性 记录总的打印次数
    volatile int printControl = 0;

    // 锁变量
    Object lock = new Object();

    public void main() {
        // 起三个线程
        ThreadPrint threadPrintA = new ThreadPrint("A", 0);
        ThreadPrint threadPrintB = new ThreadPrint("B", 1);
        ThreadPrint threadPrintC = new ThreadPrint("C", 2);

        // 这里的启动顺序没要求的
        threadPrintB.start();
        threadPrintC.start();
        threadPrintA.start();
    }

    class ThreadPrint extends Thread {
        // 当前线程要打印的变量
        String printStr;
        // 当前线程的控制变量，顺序靠他
        volatile int _printControl;

        public ThreadPrint(String printStr, int printControl) {
            super(printStr);
            this.printStr = printStr;
            this._printControl = printControl;
        }

        @Override
        public void run() {
            synchronized (lock) {
                System.out.println("name is " + getName() + "  get lock");
                // 一般只考虑打印30个
                while (printControl <= 30) {
                    System.out.println("name is " + getName() + "  in while");
                    // 必须是刚好这个线程能打印才能进这个逻辑
                    if (printControl % 3 == _printControl) {
                        System.out.println(printStr + " Current control is " + printControl);
                        printControl++;
                        // 唤醒其他线程，到就绪态
                        lock.notifyAll();
                    } else {
                        try {
                            // 自己wait，把锁交出去
                            lock.wait();
                        } catch (IllegalMonitorStateException e) {
                            System.out.println("name is " + getName() + "  " + e.toString());
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
