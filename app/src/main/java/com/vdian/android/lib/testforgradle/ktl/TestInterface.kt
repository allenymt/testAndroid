package com.vdian.android.lib.testforgradle.ktl

import android.icu.text.Transliterator

/**
 * @author yulun
 * @since 2022-01-20 10:54
 */
class TestInterface {
}

interface MyInterface {
    val prop: Int // 抽象的

    val propertyWithImplementation: String
        get() = "foo"

    fun foo() {
        print(prop)
    }
}

class Child : MyInterface {
    override val prop: Int = 29
}


interface Named {
    val name: String
}

interface Person2 : Named {
    val firstName: String
    val lastName: String

    override val name: String get() = "$firstName $lastName"
}

data class Employee(
    // 不必实现“name”
    override val firstName: String,
    override val lastName: String,
    val position: Transliterator.Position
) : Person2



// 案例，多个接口冲突
interface A {
    fun foo() { print("A") }
    fun bar()
}

interface B {
    fun foo() { print("B") }
    fun bar() { print("bar") }
}

class C : A {
    // foo方法一键实现，所以不需要继承实现
    override fun bar() { print("bar") }
}

class D : A, B {
    // A、B都声明了foo方法
    override fun foo() {
        super<A>.foo()
        super<B>.foo()
    }

    override fun bar() {
        super<B>.bar()
    }
}