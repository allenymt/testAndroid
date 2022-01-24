package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-19 17:34
 * 控制流
 */
class TestControlFlow {
    // 在 Kotlin 中，if是一个表达式，即它会返回一个值。 因此就不需要三元运算符（条件 ? 然后 : 否则），因为普通的 if 就能胜任这个角色。
    // 所以在kotlin里没有三元表达式(? :)

    fun ifElse() {
        var a = 1
        var b = 2
        var max: Int

        // 传统用法
        max = a
        if (a < b) max = b

        // With else
        if (a > b) {
            max = a
        } else {
            max = b
        }

        // 作为表达式
        max = if (a > b) a else b



        //------------
        max = if (a > b) { // 这里面可以是代码块的
            print("Choose a")
            a
        } else {
            print("Choose b")
            b
        }


    }


    fun whenT(){
        // 这种写法是真舒服
//        when (x) {
//            in 1..10 -> print("x is in the range")
//            in validNumbers -> print("x is valid")
//            !in 10..20 -> print("x is outside the range")
//            else -> print("none of the above")
//        }

        //叼的一批
        fun hasPrefix(x: Any) = when(x) {
            is String -> x.startsWith("prefix")
            else -> false
        }

        // 自 Kotlin 1.3 起，可以使用以下语法将 when 的主语（subject，译注：指 when 所判断的表达式）捕获到变量中：
        // fun Request.getBody() =
        //        when (val response = executeRequest()) {
        //            is Success -> response.body
        //            is HttpError -> throw HttpException(response.status)
        //        }
    }

    fun forT(){
        // for (item in collection) print(item)

        // 循环体可以是一个代码块。
        //for (item: Int in ints) {
        //    // ……
        //}

        for (i in 1..3) {
            println(i)
        }

        for (i in 6 downTo 0 step 2) {
            println(i)
        }
    }

    fun whileT(){
        var x=10
        while (x > 0) {
            x--
        }

//        do {
//            val y = retrieveData()
//        } while (y != null) // y 在此处可见
    }
}