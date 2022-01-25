package com.vdian.android.lib.testforgradle.ktl

import android.net.Network

/**
 * @author yulun
 * @since 2022-01-24 19:16
 * 内联类和内联方法
 * 类型别名：https://www.kotlincn.net/docs/reference/type-aliases.html
 * 内联类：https://www.kotlincn.net/docs/reference/inline-classes.html
 */
//类型别名
typealias NodeSet = Set<Network>

class TestInline {
    // 编译后，会被展开，类似于内联和拆装箱
    var testSet : NodeSet?= null
}

//内联类 start===============
interface Printable {
    fun prettyPrint(): String
}

// inline就是内联类的关键字
inline class TestInlineName(val s: String) : Printable {
    override fun prettyPrint(): String = "Let's $s!"
}

fun main() {
    // 编译时会被展开
    val testInlineName = TestInlineName("Kotlin")
    println(testInlineName.prettyPrint()) // 仍然会作为一个静态方法被调用
}

inline class UInt(val x: Int)

// 在 JVM 平台上被表示为'public final void compute(int x)'
fun compute(x: Int) { }

// 同理，在 JVM 平台上也被表示为'public final void compute(int x)'！
fun compute(x: UInt) { }


//  注意内联类会被编译保存的，只不过内部的方法(这个类特殊的方法)命名会多一串hash吗，因为如果出现多个同名内联类，那函数名就会发生错误，参考上面的例子
//  public final class TestInlineName implements Printable {
//  }
//查看编译后的代码
// public final class TestInlineKt {
//    public static final void main() {
//          m1328constructor-impl是初始化
//          m1332prettyPrint-impl具体的调用方法
//        System.out.println(TestInlineName.m1332prettyPrint-impl(TestInlineName.m1328constructor-impl("Kotlin")));
//    }
//}
//      注意到编译后的构造函数多了hash码，而且返回的是个String,因为String是内联类实际的属性值
//    /* renamed from: constructor-impl */
//    public static String m1328constructor-impl(String str) {
//        Intrinsics.checkNotNullParameter(str, "s");
//        return str;
//    }
//
//      就是TestInlineName实际实现的方法，其实就是对String做了拼接
//     /* renamed from: prettyPrint-impl */
//    public static String m1332prettyPrint-impl(String arg0) {
//        Intrinsics.checkNotNullParameter(arg0, "arg0");
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("Let's ");
//        stringBuilder.append(arg0);
//        stringBuilder.append('!');
//        return stringBuilder.toString();
//    }
//内联类 end===============