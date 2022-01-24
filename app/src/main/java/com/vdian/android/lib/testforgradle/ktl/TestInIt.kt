package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-20 09:44
 * 在 Kotlin 中所有类都有一个共同的超类 Any，这对于没有超类型声明的类是默认超类：
 * 默认情况下，Kotlin 类是最终（final）的：它们不能被继承。 要使一个类可继承，请用 open 关键字标记它。
 */
open class TestInIt {
    fun a() {
        var initDemo = Man("sdf","f","l",10)
    }
}

class Man(name: String, firstName: String, lastName: String, age: Int) :
    Person(firstName, lastName, age) {

    val customerKey = name.toUpperCase()

    val firstProperty = "First property: $name".also(::println)

    // 从父类继承
    override val sex: Int
        get() = super.sex

    init {
        println("First initializer block that prints ${name}")
    }

    val secondProperty = "Second property: ${name.length}".also(::println)

    init {
        println("Second initializer block that prints ${name.length}")
    }

    //和java一样，如果不想被子类继承，那就被final修饰
    override fun canOverride() {
        super.canOverride()
    }
}

// 可以直接把字段声明在构造函数里
open class Person(val firstName: String, val lastName: String, var age: Int) {
    // 注意这种情况会编译报错，因为已经有主构造函数了次构造函数需要委托到主构造函数上
//    var children: MutableList<Person> = mutableListOf()
//    constructor(parent: Person) : this(name){
//        parent.children.add(this)
//    }

    // 属性要想被继承，也要用open修饰
    open val sex: Int = 0

    fun notOverride(){}

    //只有被open修饰的才能被继承
    open fun canOverride(){}
}

//如果类有一个主构造函数，每个次构造函数需要委托给主构造函数，
//可以直接委托或者通过别的次构造函数间接委托。委托到同一个类的另一个构造函数用 this 关键字即可
class Person1(val name: String) {
    var children: MutableList<Person1> = mutableListOf()

    constructor(name: String, parent: Person1) : this(name) {
        parent.children.add(this)
    }
}