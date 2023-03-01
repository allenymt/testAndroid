package com.vdian.android.lib.testforgradle.ktl.collections

/**
 * @author yulun
 * @since 2022-01-26 11:
 * https://www.kotlincn.net/docs/reference/sequences.html 序列
 * 区分于迭代器
 */
class TestSequences {

    fun doIterable(){
        //执行顺序参考https://www.kotlincn.net/assets/images/reference/sequences/list-processing.png
        val words = "The quick brown fox jumps over the lazy dog".split(" ")
        val lengthsList = words.filter { println("filter: $it"); it.length > 3 }
            .map { println("length: ${it.length}"); it.length }
            .take(4) //take就是取前4个

        println("Lengths of first 4 words longer than 3 chars:")
        println(lengthsList)
    }

    fun doSequences(){
        //一句话总结，序列的执行不是列表优先，而是元素优先，比如下方的filter\map\take，是按单个元素的顺序，比如第1个元素先执行filter\map\take，然后才是第二个元素
        //https://www.kotlincn.net/assets/images/reference/sequences/sequence-processing.png执行顺序图片
        val words = "The quick brown fox jumps over the lazy dog".split(" ")
        // 将列表转换为序列
        val wordsSequence = words.asSequence()

        val lengthsSequence = wordsSequence.filter { println("filter: $it"); it.length > 3 }
            .map { println("length: ${it.length}"); it.length }
            .take(4)

        println("Lengths of first 4 words longer than 3 chars")
        // 末端操作：以列表形式获取结果。
        println(lengthsSequence.toList())
    }
}