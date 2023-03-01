package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-24 13:57
 * 又到了喜闻乐见的泛型环节
 * https://www.kotlincn.net/docs/reference/generics.html
 * 整体太绕了。。
 */
class TestGenerics {

    //简单口诀(kotlin的更好理解)
    //只读不可写时,使用List<? extends Fruit>:Producer，out，生产者(向外，out)
    //只写不可读时,使用List<? super Apple>:Consumer, in，消费者（set，被设置，被复制，写入，in）
    //消费者 in, 生产者 out

    //kotlin和java一样，也是泛型擦除的，也就是伪泛型
}