package com.vdian.android.lib.testforgradle.testclass

/**
 * @author yulun
 * @since 2022-01-17 17:40
 * 关于java class和kclass
 * kotlin 中::class 、class.java、javaClass、javaClass.kotlin区别
 * https://www.jianshu.com/p/a900ee71ae7f
 */
class JAndKClassTest {

    fun testA() {
        // in java
        // AClass aClass = new AClass();
        //Class c = AClass.getClass(); //对象获取
        // Class cc =aClass.class;//类获取


        // in kotlin
        var aClass = AClass()
        //对象获取
        var j1 = aClass.javaClass// javaClass,等同于java里的getClass()
        println("JAndKClassTest j1 is $j1")
        var j2 = aClass::class.java // javaClass，::class其实拿到的是kClass,调用java转成了java的class
        println("JAndKClassTest j2 is $j2")
        var j3 = aClass.javaClass.kotlin.java.kotlin.java// 这里可以一直嵌套 参考第一步和第二步，其实就是转来转去
        println("JAndKClassTest j3 is $j3")

        //类获取
        var k1 = AClass::class// kClass
        println("JAndKClassTest k1 is $k1")

        var k2 = (AClass::class as Any).javaClass// javaClass，拿到的是这个kotlin.jvm.internal.ClassReference
        println("JAndKClassTest k2 is $k2")

        var k3 = AClass::class.java // 先拿到KClass ,再转javaClass 同j2，一个对象，一个是类s
        println("JAndKClassTest k3 is $k3")

        var k4 = AClass::javaClass // property javaClass ，没理解
        println("JAndKClassTest k4 is $k4")
    }
}

class AClass {

}