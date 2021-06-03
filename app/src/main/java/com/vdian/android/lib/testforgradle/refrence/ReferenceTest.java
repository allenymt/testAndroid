package com.vdian.android.lib.testforgradle.refrence;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.LinkedList;

public class ReferenceTest {

    private static ReferenceQueue<Source> rq = new ReferenceQueue<Source>();

    public static void checkQueue() {
        Reference<? extends Source> ref = null;
        while ((ref = rq.poll()) != null) {
            if (ref != null) {
                if (ref instanceof SourceSoftReference) {
                    System.out.println(" ReferenceTestIn queue: " + ((SourceSoftReference) (ref)).id);
                } else if (ref instanceof SourceWeakReference) {
                    System.out.println(" ReferenceTestIn queue: " + ((SourceWeakReference) (ref)).id);
                } else {
                    System.out.println(" ReferenceTestIn queue: " + ((SourcePhantomReference) (ref)).id);
                }

            }
        }
    }

    public static void main(String args[]) {
//        soft();
//        weak();
        phantom();
    }

    public static void soft() {
        int size = 3;
        LinkedList<SourceSoftReference> softList = new LinkedList<SourceSoftReference>();
        for (int i = 0; i < size; i++) {
            softList.add(new SourceSoftReference(new Source("Soft " + i), rq));
            System.out.println(" ReferenceTestJust created soft: " + softList.getLast());
        }
        Runtime.getRuntime().gc();
        try { // 下面休息几分钟，让上面的垃圾回收线程运行完成
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        try { // 下面休息几分钟，让上面的垃圾回收线程运行完成
//            Thread.currentThread().sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        for (int i = 0; i < size; i++) {
            System.out.println(" ReferenceTestoutput: " + softList.get(i).get());
        }

        softList.clear();
        softList = null;
        Runtime.getRuntime().gc();
        try { // 下面休息几分钟，让上面的垃圾回收线程运行完成
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        checkQueue();
    }

    public static void weak() {
        int size = 3;
        LinkedList<SourceWeakReference> weakList = new LinkedList<SourceWeakReference>();
        for (int i = 0; i < size; i++) {
            weakList.add(new SourceWeakReference(new Source("Weak " + i), rq));
            System.out.println(" ReferenceTestJust created weak: " + weakList.getLast());

        }
        Runtime.getRuntime().gc();
        try { // 下面休息几分钟，让上面的垃圾回收线程运行完成
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < size; i++) {
            System.out.println(" ReferenceTestouput: " + i + weakList.get(i).get());
        }
        checkQueue();
    }

    public static void phantom() {
        int size = 3;
        LinkedList<SourcePhantomReference> phantomList = new LinkedList<SourcePhantomReference>();
        for (int i = 0; i < size; i++) {
            phantomList.add(new SourcePhantomReference(new Source("Phantom " + i), rq));
            System.out.println(" ReferenceTestJust created phantom: " + phantomList.getLast());

        }
        Runtime.getRuntime().gc();
        try { // 下面休息几分钟，让上面的垃圾回收线程运行完成
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < size; i++) {
            System.out.println(" ReferenceTestoutput: " + phantomList.get(i).get());
        }
        checkQueue();
    }

}

class Source {
    public String id;

    public Source(String id) {
        this.id = id;
    }

//    protected void finalize() {
////        System.out.println(" ReferenceTestFinalizing Source " + id);
//    }
}

class SourceSoftReference extends SoftReference<Source> {
    public String id;

    public SourceSoftReference(Source Source, ReferenceQueue<Source> rq) {
        super(Source, rq);
        this.id = Source.id;
    }
}

class SourceWeakReference extends WeakReference<Source> {
    public String id;

    public SourceWeakReference(Source Source, ReferenceQueue<Source> rq) {
        super(Source, rq);
        this.id = Source.id;
    }
}

class SourcePhantomReference extends PhantomReference<Source> {
    public String id;

    public SourcePhantomReference(Source Source, ReferenceQueue<Source> rq) {
        super(Source, rq);
        this.id = Source.id;
    }

}
