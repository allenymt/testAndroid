package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-20 11:20
 * 扩展:
 *  扩展函数
 */

// Kotlin 能够扩展一个类的新功能而无需继承该类或者使用像装饰者这样的设计模式。
// 这通过叫做 扩展 的特殊声明完成。 例如，你可以为一个你不能修改的、来自第三方库中的类编写一个新的函数。
// 这个新增的函数就像那个原始类本来就有的函数一样，可以用普通的方法调用。 这种机制称为 扩展函数 。
// 此外，也有 扩展属性 ， 允许你为一个已经存在的类添加新的属性。

// 扩展不能真正的修改他们所扩展的类。通过定义一个扩展，你并没有在一个类中插入新成员， 仅仅是可以通过该类型的变量用点表达式去调用这个新函数
// 对于扩展属性，属性并不能真的被扩展，只是通过他们的get和set方法来支持，本质上还是方法扩展
class TestExtensions {
    fun testExtendMutableList(){
        val list = mutableListOf(1, 2, 3)
        list.swapF(0, 2) // “swap()”内部的“this”会保存“list”的值
        var a = list[0]
    }

    fun doSomeThing(){
        println(123)
    }
}

// 声明一个扩展函数，我们需要用一个 接收者类型，
// 也就是被扩展的类型来作为他的前缀。 下面代码为 MutableList<Int> 添加一个swap 函数：
// index1和index2是入参
fun MutableList<Int>.swap(index1: Int, index2: Int) {
    // 这里的this代表什么呢？代表当前调用的MutableList对象
    val tmp = this[index1] // “this”对应该列表
    this[index1] = this[index2]
    this[index2] = tmp
}

// 泛型扩展
fun <T> MutableList<T>.swapF(index1: Int, index2: Int) {
    val tmp = this[index1] // “this”对应该列表
    this[index1] = this[index2]
    this[index2] = tmp
}

fun TestExtensions.doPrint(msg:String){
    println(msg)
}


//所以扩展函数的原理是什么？是真的替换了原有类
//先看编译后的产物，可以看到编译后新增了一个类,类的命名是原类名+Kt
//public final class TestExtensionsKt {
//    看这个方法更直白
//    public static final void swap(List<Integer> $this$swap, int index1, int index2) {
//        Intrinsics.checkNotNullParameter($this$swap, "<this>");
//        $this$swap就是调用方法的原始对象
//        int tmp = ((Number) $this$swap.get(index1)).intValue();
//        $this$swap.set(index1, $this$swap.get(index2));
//        $this$swap.set(index2, Integer.valueOf(tmp));
//    }
//
//    public static final <T> void swapF(List<T> $this$swapF, int index1, int index2) {
//        Intrinsics.checkNotNullParameter($this$swapF, "<this>");
//        Object tmp = $this$swapF.get(index1);
//        $this$swapF.set(index1, $this$swapF.get(index2));
//        $this$swapF.set(index2, tmp);
//    }
//
//    新生成的方法里，除了原始函数，还有原始类的对象
//    public static final void doPrint(TestExtensions $this$doPrint, String msg) {
//        Intrinsics.checkNotNullParameter($this$doPrint, "<this>");
//        Intrinsics.checkNotNullParameter(msg, NotificationCompat.CATEGORY_MESSAGE);
//        System.out.println(msg);
//    }
//}

//再看扩展函数编译后是怎么调用的
//  public final void testCallExtension() {
//        第一个入参都是原始对象
//        TestExtensionsKt.swap(CollectionsKt__CollectionsKt.mutableListOf(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)), 0, 2);
//        TestExtensionsKt.doPrint(new TestExtensions(), "sdf");
//    }

//对于扩展属性，扩展了list的size的get方法，说以本质上还是扩展方法
val <T> List<T>.lastIndex: Int
    get() = size - 1




// 扩展是静态分析的
// 扩展不能真正的修改他们所扩展的类。通过定义一个扩展，你并没有在一个类中插入新成员， 仅仅是可以通过该类型的变量用点表达式去调用这个新函数。
//我们想强调的是扩展函数是静态分发的，即他们不是根据接收者类型的虚方法。 这意味着调用的扩展函数是由函数调用所在的表达式的类型来决定的， 而不是由表达式运行时求值结果决定的
//class Example {
//    fun printFunctionType() { println("Class method") }
//}
//
//fun Example.printFunctionType() { println("Extension function") }
//
//Example().printFunctionType()