package com.vdian.android.lib.testforgradle.thread;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yulun
 * @sinice 2020-03-18 11:31
 */
public class TestPrintAbc {
    volatile int printControl = 1;

    public void main() {
        ThreadPrint threadPrintA = new ThreadPrint("A", 1);
        ThreadPrint threadPrintB = new ThreadPrint("B", 2);
        ThreadPrint threadPrintC = new ThreadPrint("C", 0);
        threadPrintA.start();
        threadPrintB.start();
        threadPrintC.start();
    }
    static AtomicBoolean atomicBoolean = new AtomicBoolean(false);
    class ThreadPrint extends Thread {
        String printStr;
        volatile int _printControl;

        public ThreadPrint(String printStr, int printControl) {
            super(printStr);
            this.printStr = printStr;
            this._printControl = printControl;
        }

        @Override
        public void run() {
            if (atomicBoolean.compareAndSet(false, true)) {
                android.util.Log.i("TestPrintAbc", "atomicBoolean zzz");
            }
            synchronized (ThreadPrint.class) {
                System.out.println("name is " + getName() + "  get lock");
                while (printControl <= 30) {
                    System.out.println("name is " + getName() + "  in while");
                    if (printControl % 3 == _printControl) {
                        System.out.println(printStr + " Current control is " + printControl);
                        printControl++;
                        ThreadPrint.class.notifyAll();
                    } else {
                        try {
                            ThreadPrint.class.wait();
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
