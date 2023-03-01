package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-20 10:10
 * 内部类测试
 */
class TestInner {
}

open class Rectangle {
    open fun draw() { println("Drawing a rectangle") }
    val borderColor: String get() = "black"
}

class FilledRectangle: Rectangle() {
    override fun draw() {
        val filler = Filler()
        filler.drawAndFill()
    }

    inner class Filler {
        fun fill() { println("Filling") }
        fun drawAndFill() {
            super@FilledRectangle.draw() // 调用 Rectangle 的 draw() 实现，也就是外部类的附列的实现
            fill()
            println("Drawn a filled rectangle with color ${super@FilledRectangle.borderColor}") // 使用 Rectangle 所实现的 borderColor 的 get()
        }
    }
}

interface Polygon {
    // 接口成员默认就是“open”的
    fun draw() { /* …… */ }
}

// 同时继承一个类和接口
class Square() : Rectangle(), Polygon {
    // 编译器要求覆盖 draw()： ,两个超类必须都覆盖
    override fun draw() {
        super<Rectangle>.draw() // 调用 Rectangle.draw()
        super<Polygon>.draw() // 调用 Polygon.draw()
    }
}