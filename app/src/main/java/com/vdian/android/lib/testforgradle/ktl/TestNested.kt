package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-24 15:24
 * 嵌套类与内部类
 */
class TestNested {
    class Outer {
        private val bar: Int = 1
        class Nested {
            fun foo() = 2
        }

        inner class Inner {
            fun foo() = bar
        }
    }

    val demo = Outer.Nested().foo() // == 2
    val demo1 = Outer().Inner().foo() // == 1
}