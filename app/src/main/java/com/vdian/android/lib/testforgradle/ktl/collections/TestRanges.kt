package com.vdian.android.lib.testforgradle.ktl.collections

/**
 * @author yulun
 * @since 2022-01-26 11:17
 */
class TestRanges {
    fun a(){
        for (i in 1..4) print(i)

        // 反向迭代
        for (i in 4 downTo 1) print(i)

        // 自定义步长
        for (i in 1..8 step 2) print(i)
        println()
        for (i in 8 downTo 1 step 2) print(i)

        // 要迭代不包含其结束元素的数字区间，请使用 until 函数
        for (i in 1 until 10) {       // i in [1, 10), 10被排除
            print(i)
        }
    }

    fun b(){

    }
}