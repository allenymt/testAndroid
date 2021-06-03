package com.vdian.android.lib.testforgradle.thread;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yulun
 * @sinice 2020-03-14 17:59
 */
public class Producer {
    List<String> itemList = new ArrayList<>(5);
    Object lock = new Object();

    public void main() {
        ThreadProducer threadProducer = new ThreadProducer();

        ThreadCustomer threadCustomer = new ThreadCustomer();
        threadProducer.start();
        threadCustomer.start();
    }

    class ThreadProducer extends Thread {

        public ThreadProducer() {
            super("producer");
        }

        @Override
        public void run() {
            synchronized (lock) {
                while (true) {
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        itemList.add("item" + i);
                        System.out.println("producer item " + itemList.get(i));
                    }
                    lock.notify();
                    try {
                        lock.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class ThreadCustomer extends Thread {

        public ThreadCustomer() {
            super("customer");
        }

        @Override
        public void run() {
            synchronized (lock) {
                while (true) {
                    for (int i = 0; i < itemList.size(); i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("custom item " + itemList.get(i));
                    }
                    itemList.clear();
                    lock.notify();
                    try {
                        lock.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
