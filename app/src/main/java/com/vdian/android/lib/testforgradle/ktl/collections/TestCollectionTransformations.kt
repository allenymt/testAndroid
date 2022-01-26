package com.vdian.android.lib.testforgradle.ktl.collections

/**
 * @author yulun
 * @since 2022-01-26 14:46
 * 集合转换：https://www.kotlincn.net/docs/reference/collection-transformations.html
 */
class TestCollectionTransformations {
    //就是遍历元素转换
    fun doMap(){
        val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
        println(numbersMap.mapKeys { it.key.toUpperCase() })
        println(numbersMap.mapValues { it.value + it.key.length })
    }

    //合并两个list, 各取一个元素转换为pair
    fun doZip(){
        val colors = listOf("red", "brown", "grey")
        val animals = listOf("fox", "bear", "wolf")
        println(colors zip animals)

        val twoAnimals = listOf("fox", "bear")
        println(colors.zip(twoAnimals))
    }

    // 关联
    fun doAssociate(){
        val numbers = listOf("one", "two", "three", "four")

        println(numbers.associateBy { it.first().toUpperCase() })
        println(numbers.associateBy(keySelector = { it.first().toUpperCase() }, valueTransform = { it.length }))
    }

    //展开元素里的list
    fun doFlat(){
        val numberSets = listOf(setOf(1, 2, 3), setOf(4, 5, 6), setOf(1, 2))
        println(numberSets.flatten())
    }

    //过滤
    fun doFilter(){
        val numbers = listOf("one", "two", "three", "four")
        val longerThan3 = numbers.filter { it.length > 3 }
        println(longerThan3)

        val numbersMap = mapOf("key1" to 1, "key2" to 2, "key3" to 3, "key11" to 11)
        val filteredMap = numbersMap.filter { (key, value) -> key.endsWith("1") && value > 10}
        println(filteredMap)
    }
}