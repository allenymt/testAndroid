package com.vdian.android.lib.testforgradle.reflex

import java.lang.reflect.Method

/**
 * @author yulun
 * @sinice 2021-12-14 17:24
 */
class KtTestReflex {
    object KtTestReflex{
        fun test(){
            try {
                val cls: Class<*> = Class.forName("com.vdian.android.lib.testforgradle.reflex.Test")
                val method: Method = cls.getDeclaredMethod("testReturnStr")
                method.isAccessible = true
//                val bn = method.invoke(null,null)//报错,java里第二个参数传null就没问题
                // kotlin编译之后是 Log.e("KtTestReflex", String.valueOf(method.invoke(null, new Object[]{null})));，kotlin把null放到数组里了
                // 因为java里编译后加了个强转Object ss = m.invoke((Object)null, (Object[])null);，坑爹啊 ,java是把null转为数组

                //val bn = method.invoke(null) 可以
                val bn = method.invoke(null,*arrayOf<Any>()) // 在kotlin里，反射时如果入参是可变数组，即使参数为空，也要跟着一起声明
                // Log.e("KtTestReflex", String.valueOf(method.invoke(null, new Object[0])));

                android.util.Log.e("KtTestReflex","$bn")
            } catch (e: Exception) {
            }
        }
    }
}