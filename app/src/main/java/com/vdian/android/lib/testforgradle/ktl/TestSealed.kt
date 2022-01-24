package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-24 11:48
 * 密封类,略过吧，可能是案例的原因，感觉没什么用
 * 密封类用来表示受限的类继承结构：当一个值为有限几种的类型、而不能有任何其他类型时。在某种意义上，他们是枚举类的扩展：枚举类型的值集合也是受限的，但每个枚举常量只存在一个实例，而密封类的一个子类可以有可包含状态的多个实例
 */
class TestSealed {
    fun eval(expr: Expr): Double = when(expr) {
        is Const -> expr.number
        is Sum -> eval(expr.e1) + eval(expr.e2)
        NotANumber -> Double.NaN
        // 不再需要 `else` 子句，因为我们已经覆盖了所有的情况
    }
}

sealed class Expr
data class Const(val number: Double) : Expr()
data class Sum(val e1: Expr, val e2: Expr) : Expr()
object NotANumber : Expr()