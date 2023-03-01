package com.vdian.android.lib.testforgradle.ktl.collections

import java.util.*
import kotlin.collections.HashSet

/**
 * @author yulun
 * @since 2022-01-26 10:59
 * https://www.kotlincn.net/docs/reference/constructing-collections.html
 * 集合构造方法
 */
class TestConstructingCollections {
    fun a(){
        val numbersSet = setOf("one", "two", "three", "four")
        val emptySet = mutableSetOf<String>()
        val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key4" to 1)
        val empty = emptyList<String>()
        //创建没有任何元素的集合的函数：emptyList()、emptySet() 与 emptyMap()。 创建空集合时，应指定集合将包含的元素类型。

        val doubled = List(3, { it * 2 })  // 如果你想操作这个集合，应使用 MutableList
        println(doubled)


        val linkedList = LinkedList<String>(listOf("one", "two", "three"))
        val presizedSet = HashSet<Int>(32)


        // toMutableList和toList都是clone
        val sourceList = mutableListOf(1, 2, 3)
        val copyList = sourceList.toMutableList()
        val readOnlyCopyList = sourceList.toList()
        sourceList.add(4)
        println("Copy size: ${copyList.size}")
        //readOnlyCopyList.add(4)             // 编译异常
        println("Read-only copy size: ${readOnlyCopyList.size}")
    }

    fun b(){
        //注意可变集合和不可变集合
        val sourceList = mutableListOf(1, 2, 3)
        val referenceList: List<Int> = sourceList //默认是不可变的，没有add remove操作
//referenceList.add(4)            // 编译错误
        sourceList.add(4)
        println(referenceList) // 显示 sourceList 当前状态
    }
}