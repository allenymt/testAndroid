package com.vdian.android.lib.testforgradle.ktl.coroutines

/**
 * @author yulun
 * @since 2022-01-27 17:07
 */
fun log(msg:Any){
    println("CoroutinesLog: thread-name:${Thread.currentThread().name} , $msg")
}