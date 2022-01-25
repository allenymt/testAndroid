package com.vdian.android.lib.testforgradle.ktl.inline

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * @author yulun
 * @since 2022-01-24 20:01
 * 内联函数： https://www.kotlincn.net/docs/reference/inline-functions.html
 * 本质上就是展开函数，增加调用速度，但同时也要避免内联函数过长，因为内联函数会被扩展到每个调用它的地方，这意味着编译后会增加代码量
 * 参考文章：https://juejin.cn/post/6844904201353429006
 */
class TestInlineMethod {
    //在Kotlin中，lambda表达式会被正常的编译成匿名类。这表示每调用一次lambda表达式，
    // 一个额外的类就会被创建。并且如果lambda捕捉了某个变量，那么每次调用都会创建一个新的对象。
    // 这会带来运行时额外开销，导致使用lambda比使用一个直接执行相同代码的函数效率低
}

class TestNoInline{
    /**
     * 定义一个线程安全的方法
     */
    fun <T> threadSafeMethod(lock: Lock, action: () -> T): T {
        lock.lock()
        try {
            return action()
        } finally {
            println("unlock")
            lock.unlock()
        }
    }

    fun main() {
        foo(ReentrantLock(),1)
        foo(ReentrantLock(),2)
    }

    fun foo(l: Lock,num:Int) {
        //2022-01-25 11:24:49.637 15867-15867/com.vdian.android.lib.testforgradle I/System.out: Before sync
        //2022-01-25 11:24:49.638 15867-15867/com.vdian.android.lib.testforgradle I/System.out: Action
        //2022-01-25 11:24:49.638 15867-15867/com.vdian.android.lib.testforgradle I/System.out: unlock
        //2022-01-25 11:24:49.638 15867-15867/com.vdian.android.lib.testforgradle I/System.out: After sync
        println("Before sync")
        threadSafeMethod(l) {
            println("Action,$num")
        }
        println("After sync")
    }
}

//先看没有内联编译代码
//首先生成了一个独立的类，kotlin里Lambda表达式被编译成了独立类，这个类实现接口Function0,具体实现就是invoke方法
//final class TestNoInline$foo$1 extends Lambda implements Function0<Unit> {
//    final /* synthetic */ int $num;
//
//    TestNoInline$foo$1(int i) {
//        this.$num = i;
//        super(0);
//    }
//
//    public final void invoke() {
//        System.out.println(Intrinsics.stringPlus("Action,", Integer.valueOf(this.$num)));
//    }
//}

//public final class TestNoInline {
//    public final <T> T threadSafeMethod(Lock lock, Function0<? extends T> action) {
//        String str = "unlock";
//        Intrinsics.checkNotNullParameter(lock, "lock");
//        Intrinsics.checkNotNullParameter(action, "action");
//        lock.lock();
//        try {
//            T invoke = action.invoke();
//            return invoke;
//        } finally {
//            System.out.println(str);
//            lock.unlock();
//        }
//    }
//
//    public final void main() {
//        foo(new ReentrantLock(), 1);
//        foo(new ReentrantLock(), 2);
//    }
//
//    public final void foo(Lock l, int num) {
//        Intrinsics.checkNotNullParameter(l, "l");
//        System.out.println("Before sync");
//        threadSafeMethod(l, new TestNoInline$foo$1(num)); // 实际的类型是个Function0，每次调用都会重新生成一个类
//        System.out.println("After sync");
//    }
//}


// 内联后的代码
class TestRealInline{
    /**
     * 定义一个线程安全的方法
     */
    inline fun <T> threadSafeMethod(lock: Lock, action: () -> T): T {
        lock.lock()
        try {
            return action()
        } finally {
            println("unlock")
            lock.unlock()
        }
    }

    fun main() {
        foo(ReentrantLock(),1)
        foo(ReentrantLock(),2)
    }

    fun foo(l: Lock,num:Int) {
        println("Before sync")
        threadSafeMethod(l) {
            println("Action,$num")
        }
        println("After sync")
    }
}
//查看内联后编译代码,可以看到内联函数直接在方法里展开了，避免了内部类的创建
// public final void foo(Lock l, int num) {
//        String str = "unlock";
//        Intrinsics.checkNotNullParameter(l, "l");
//        System.out.println("Before sync");
//        l.lock();
//        try {
//            System.out.println(Intrinsics.stringPlus("Action,", Integer.valueOf(num)));
//            Unit unit = Unit.INSTANCE;
//            System.out.println("After sync");
//        } finally {
//            System.out.println(str);
//            l.unlock();
//        }
//    }

//番外，java是怎么优化Lambda的
//既然 Kotlin 的 Lambda 存在性能问题，那旁边的 Java 大兄弟肯定也逃脱不了。
//从 Java8 开始，Java 借助 invokedynamic 来完成的 Lambda 的优化。
//invokedynamic  用于支持动态语言调用。在首次调用时，它会生成一个调用点，并绑定该调用点对应的方法句柄。后续调用时，直接运行该调用点对应的方法句柄即可。说直白一点，第一次调用 invokeddynamic 时，会找到此处应该运行的方法并绑定， 后续运行时就直接告诉你这里应该执行哪个方法。
//关于 invokedynamic 的详细介绍，可以阅读极客时间专栏 《深入拆解Java虚拟机》的第 8,9 两讲。