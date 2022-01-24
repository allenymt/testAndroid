package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-20 10:17
 * kotlin中的属性声明
 */

//顶层常量生成了一个独立类，当前类名+Kt
//public final class TestPropertiesKt {
//    public static final int aa = 1;
//    public static final int dd = 2;
//}
const val aa = 1
const val dd = 2

class TestProperties {
    // Kotlin 类中的属性既可以用关键字 var 声明为可变的，也可以用关键字 val 声明为只读的。

    // 声明一个属性的完整语法是
    // var <propertyName>[: <PropertyType>] [= <property_initializer>]
    //    [<getter>]
    //    [<setter>]
    // 其初始器（initializer）、getter 和 setter 都是可选的。属性类型如果可以从初始器 （或者从其 getter 返回值，如下文所示）中推断出来，也可以省略。
    // 举个例子
    // 这一行是直接报错的： var allByDefault: Int? // 错误：需要显式初始化器，隐含默认 getter 和 setter
     var initialized = 1 // 类型 Int、默认 getter 和 setter
        get() = initialized
        set(value) {
            field =value
        }

    // val simple: Int? // 类型 Int、默认 getter、必须在构造函数中初始化
    val inferredType = 1 // 类型 Int 、默认 getter

    val isEmpty: Boolean // val不可变，所以不能设置setter
        get() = true

    object  obb{
        // 编译后在当前类下生成了静态内部类
        // public static final class obb {
        //        public static final obb INSTANCE = new obb();
        //        public static final int bb = 1;
        //
        //        private obb() {
        //        }
        //    }
        const val bb = 1
    }
    companion object{
        // 编译后直接在当前类下生成了一个静态常量
        // public static final int cc = 1;
        const val cc = 1
    }
}

class Address {
    var name: String = "Holmes, Sherlock"
    var street: String = "Baker"
    var city: String = "London"
    var state: String? = null
    var zip: String = "123456"

    fun copyAddress(address: Address): Address {
        val result = Address() // Kotlin 中没有“new”关键字
        result.name = address.name // 将调用访问器
        result.street = address.street
        // ……
        return result
    }

    fun b(){
        var b = aa //取的是当前文件最顶层的aa
        var c = TestProperties.cc // companion object里的aa
        var d = TestProperties.obb.bb // object  obb里的aa

//        LateTest::subject.isLateinit
    }

    var lateTest:LateTest? = null

}

//该修饰符只能用于在类体中的属性（不是在主构造函数中声明的 var 属性，并且仅当该属性没有自定义 getter 或 setter 时），
// 而自 Kotlin 1.2 起，也用于顶层属性与局部变量。该属性或变量必须为非空类型，并且不能是原生类型。
//在初始化前访问一个 lateinit 属性会抛出一个特定异常，该异常明确标识该属性被访问及它没有初始化的事实
public class LateTest {
    lateinit var subject: Address

    fun setup() {
        subject = Address()
    }

    fun test() {
        subject.b()  // 直接解引用
    }

    // 如何检测一个 lateinit var 是否已初始化 TODO ,没看懂
    // if (foo::bar.isInitialized) {
    //    println(foo.bar)
    //}
}