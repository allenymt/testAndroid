package com.vdian.android.lib.testforgradle.ktl

import com.vdian.android.lib.testforgradle.ktl.extensions.TestExtensions
import com.vdian.android.lib.testforgradle.ktl.extensions.doPrint
import com.vdian.android.lib.testforgradle.ktl.extensions.swap

/**
 * @author yulun
 * @since 2022-01-19 17:26
 */
class TestBasicTypes {

    // 基本类型
    // 数字
    //Kotlin 提供了一组表示数字的内置类型。 对于整数，有四种不同大小的类型，因此值的范围也不同。
    //
    //类型	大小（比特数）	最小值	最大值
    //Byte	8	-128	127
    //Short	16	-32768	32767
    //Int	32	-2,147,483,648 (-231)	2,147,483,647 (231 - 1)
    //Long	64	-9,223,372,036,854,775,808 (-263)	9,223,372,036,854,775,807 (263 - 1)

    //类型	大小（比特数）	有效数字比特数	指数比特数	十进制位数
    //Float	32	24	8	6-7
    //Double	64	53	11	15-16

    //字符 char
    //布尔 布尔用 Boolean 类型表示，它有两个值：true 与 false

    //数组 数组在 Kotlin 中使用 Array 类来表示，它定义了 get 与 set 函数（按照运算符重载约定这会转变为 []）以及 size 属性，以及一些其他有用的成员函数：
    //使用库函数 arrayOf() 来创建一个数组并传递元素值给它

    //原生类型数组  ByteArray、 ShortArray、IntArray 等等

    //字符串 用 String 类型表示。字符串是不可变的。 字符串的元素——字符可以使用索引运算符访问: s[i]。 可以用 for 循环迭代字符串:
    fun test1() {
        //这是完整的位运算列表（只用于 Int 与 Long）：
        //
        //shl(bits) – 有符号左移
        //shr(bits) – 有符号右移
        //ushr(bits) – 无符号右移
        //and(bits) – 位与
        //or(bits) – 位或
        //xor(bits) – 位异或
        //inv() – 位非
        val x = (1 shl 2) and 0x000FF000


        // 本节讨论的浮点数操作如下：
        //
        //相等性检测：a == b 与 a != b
        //比较操作符：a < b、 a > b、 a <= b、 a >= b
        //区间实例以及区间检测：a..b、 x in a..b、 x !in a..b
    }

    fun testCallExtension() {
        val list = mutableListOf(1, 2, 3)
        list.swap(0, 2) // “swap()”内部的“this”会保存“list”的值


        var testExtensions = TestExtensions()
        testExtensions.doPrint("sdf")
    }
}