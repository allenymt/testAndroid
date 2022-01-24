package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-24 15:36
 * object和companion
 * 对象和伴生对象
 *
 * https://www.kotlincn.net/docs/reference/object-declarations.html
 *  对象表达式是在使用他们的地方立即执行（及初始化）的；(伴生对象的实例声明是在内部，所以在使用的地方才初始化)
对象声明是在第一次被访问到时延迟初始化的；
伴生对象的初始化是在相应的类被加载（解析）时，与 Java 静态初始化器的语义相匹配。(主类被加载的时候就初始化了)
 */
class TestObjectCompanion {
    val bbb =2
    //对象和伴生对象有什么区别？ 编译后区别是什么呢？？

    //对于对象，关键字就是object,一般在匿名内部类的时候用的很多，比如给view设置监听
    //另外，object个人在写代码的时候用的最多的就是util类的写法,注意这个对象可以继承其他类的
    object ObjectManager {
        val b=1
        fun registerDataProvider() {
        }
    }

    //分割线，总结一句话就是替代java里的static
    //伴生对象 companion，替代java里的static，因为伴生对象的实例编译后在外部，而且是静态的，所以和java里的static一样
    companion object CompanionFactory {
        fun create(): TestObjectCompanion = TestObjectCompanion()

        val a=1

        fun ccccd(){
        }
    }


    fun A() {
        ObjectManager.registerDataProvider()
        CompanionFactory.create()
    }
}

// 一起看看编译后的代码
// /* compiled from: TestObjectCompanion.kt */
// public final class TestObjectCompanion {
//      果然是伴生类，实例和属性都被定义在外面。。。怪不得伴生类只能有一个
//    public static final CompanionFactory CompanionFactory = new CompanionFactory();
//    private static final int a = 1;
//
//    @Metadata(d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\b\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u0006\u0010\u0007\u001a\u00020\bR\u0014\u0010\u0003\u001a\u00020\u0004XD¢\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006¨\u0006\t"}, d2 = {"Lcom/vdian/android/lib/testforgradle/ktl/TestObjectCompanion$CompanionFactory;", "", "()V", "a", "", "getA", "()I", "create", "Lcom/vdian/android/lib/testforgradle/ktl/TestObjectCompanion;", "app_debug"}, k = 1, mv = {1, 5, 1}, xi = 48)
//    /* compiled from: TestObjectCompanion.kt */
//    public static final class CompanionFactory {
//        public /* synthetic */ CompanionFactory(DefaultConstructorMarker defaultConstructorMarker) {
//            this();
//        }
//
//        private CompanionFactory() {
//        }
//
//        public final TestObjectCompanion create() {
//            return new TestObjectCompanion();
//        }
//
//        public final int getA() {
//            return TestObjectCompanion.a;
//        }
//        public final void ccccd() {
//        }
//    }
//
//    @Metadata(d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\bÆ\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u0006\u0010\u0007\u001a\u00020\bR\u0014\u0010\u0003\u001a\u00020\u0004XD¢\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006¨\u0006\t"}, d2 = {"Lcom/vdian/android/lib/testforgradle/ktl/TestObjectCompanion$ObjectManager;", "", "()V", "b", "", "getB", "()I", "registerDataProvider", "", "app_debug"}, k = 1, mv = {1, 5, 1}, xi = 48)
//    /* compiled from: TestObjectCompanion.kt */
//    public static final class ObjectManager {
//        public static final ObjectManager INSTANCE = new ObjectManager();
//          变量是静态的
//        private static final int b = 1;
//
//        private ObjectManager() {
//        }
//
//          方法不是静态的
//        public final int getB() {
//            return b;
//        }
//
//        public final void registerDataProvider() {
//        }
//    }
//
//    public final void A() {
//        ObjectManager.INSTANCE.registerDataProvider();
//        CompanionFactory.create();
//    }
//}