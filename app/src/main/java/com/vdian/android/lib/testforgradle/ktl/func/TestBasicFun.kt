package com.vdian.android.lib.testforgradle.ktl.func

import android.view.View
import android.widget.Button

/**
 * @author yulun
 * @since 2022-01-25 16:27
 * https://www.kotlincn.net/docs/reference/functions.html
 * 只记录一些不常用的
 */

//定义一个函数，和flutter很像，参数一是button,参数二是View，返回值为空
typealias ClickHandler = (Button, View) -> Unit

class TestBasicFun {

    // 默认参数示例
    fun read(
        b: Array<Byte>,
        off: Int = 0,
        len: Int = b.size,
    ) { /*……*/
    }


    // 返回 Unit 的函数，等于不返回
    fun printHello(name: String?): Unit {
        if (name != null)
            println("Hello $name")
        else
            println("Hi there!")
        // `return Unit` 或者 `return` 是可选的
    }

    // 单表达式函数
    fun double(x: Int): Int = x * 2

    val list = asList(1, 2, 3)

    //可变数量的参数（Varargs）
    fun <T> asList(vararg ts: T): List<T> {
        val result = ArrayList<T>()
        for (t in ts) // ts is an Array
            result.add(t)
        return result
    }


    //高阶函数
    fun testHigh() {
        val items = listOf(1, 2, 3, 4, 5)

        // Lambdas 表达式是花括号括起来的代码块。
        // fold的源码写的真漂亮
        items.fold(0, {
            // 如果一个 lambda 表达式有参数，前面是参数，后跟“->”
                acc: Int, i: Int ->
            print("acc = $acc, i = $i, ")
            val result = acc + i
            println("result = $result")
            // lambda 表达式中的最后一个表达式是返回值：
            result
        })

        // lambda 表达式的参数类型是可选的，如果能够推断出来的话：
        val joinedToString = items.fold("Elements:", { acc, i -> acc + " " + i })

        // 函数引用也可以用于高阶函数调用：
        val product = items.fold(1, Int::times)

    }
}

//函数类型实例化
//使用实现函数类型接口的自定义类的实例
// 这里要补充下理解，不然还真看不懂，编译后看下产物如何生成的
class IntTransformer : (Int) -> Int { //这一行，定义了一个类，继承自函数(Int) -> Int
    override operator fun invoke(x: Int): Int = TODO() //定义了函数的操作？？
}
val intFunction: (Int) -> Int = IntTransformer() //声明一个函数=IntTransformer

//来看编译后的代码，很有意思
//编译后 IntTransformer类其实是实现了Function1接口
//public final class IntTransformer implements Function1<Integer, Integer> {
//    public Integer invoke(int x) {
//        throw new NotImplementedError(null, 1, null);
//    }
//}
//intFunction变量在定义里其实就是个Function1
// private static final Function1<Integer, Integer> intFunction = new IntTransformer();
//
//    public static final Function1<Integer, Integer> getIntFunction() {
//        return intFunction;
//    }