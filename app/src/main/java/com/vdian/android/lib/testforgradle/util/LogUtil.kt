package com.vdian.android.lib.testforgradle.util

/**
 * @author yulun
 * @since 2022-08-15 14:23
 */
class LogUtil {
    object Log{
       fun log(msg:String){
           android.util.Log.d("JustTest",msg)
       }

        fun d(tag:String , msg:String){
            android.util.Log.d("JustTest-$tag",msg)
        }
    }
}