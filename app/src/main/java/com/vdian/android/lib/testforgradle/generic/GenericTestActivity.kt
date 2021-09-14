package com.vdian.android.lib.testforgradle.generic

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.R
import java.lang.Exception
import java.util.ArrayList

class GenericTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generic_test)
    }

    // 测试泛型擦除
    fun testFxcc(view: View) {
        try {
            val childTest = GenericTest<Any?>()
            val refelex: Class<*> = childTest.javaClass
            val field = refelex.getDeclaredField("testParam")
            field.isAccessible = true
            field[childTest] = "123"
            Log.i("test reflex", childTest.getTestParam().toString() + "")
        } catch (e: Exception) {
            Log.i("test reflex", e.toString())
        }
    }


    fun testPesc(view : View){
        //通配符 具体泛型 java版本
        TestPesc.testPesc();

//        //通配符 具体泛型 object,kotlin 版本
//        val list1: ArrayList<Any> = ArrayList()
//        list1.add("hello")
//        list1.add(0)
//
//        val list2: ArrayList<String> = ArrayList()
//        list2.add("world")
//
//        val list3: ArrayList<out Number?> = ArrayList<Int?>()
//        val a: Number = Integer.valueOf(1)
//        list3.add(a)
//        val number = list3[0]
//
//        val c: GenericTest<Boolean?> = GenericTest<Boolean?>()
//        c.setTestParam(true)
    }
}