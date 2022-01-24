package com.vdian.android.lib.testforgradle.ktl

/**
 * @author yulun
 * @since 2022-01-19 19:35
 */
class TestReturns {
    // kotlin里还有个标签功能，要看看字节码是怎么运行的
    //
    fun a() {
        loop@ for (i in 1..100) {
            for (j in 1..100) {
                if (j % 2 == 0) break@loop
            }
        }

        // 没有标签的时候
        //  public final void a() {
        //        int i = 1;
        //        do {
        //            int i2 = i;
        //            i++;
        //            int i3 = 1;
        //            do {
        //                int j = i3;
        //                i3++;
        //                if (j % 2 != 0) {
        //                    break;
        //                }
        //                break;
        //            } while (i3 <= 100);
        //            continue;
        //        } while (i <= 100);
        //    }

        //有标签的时候,其实就是把break改成了return
        //int i = 1;
        //        do {
        //            int i2 = i;
        //            i++;
        //            int i3 = 1;
        //            do {
        //                int j = i3;
        //                i3++;
        //                if (j % 2 == 0) {
        //                    return;
        //                }
        //            } while (i3 <= 100);
        //        } while (i <= 100);
    }

    fun b() {
        fun foo() {
            listOf(1, 2, 3, 4, 5).forEach {
                if (it == 3) return // 非局部直接返回到 foo() 的调用者
                print(it)
            }
            println("this point is unreachable")
        }

        fun foo1() {
            listOf(1, 2, 3, 4, 5).forEach lit@{
                if (it == 3) return@lit // 局部返回到该 lambda 表达式的调用者，即 forEach 循环
                print(it)
            }
            print(" done with explicit label")
        }

        //同foo1
        fun foo2() {
            listOf(1, 2, 3, 4, 5).forEach {
                if (it == 3) return@forEach // 局部返回到该 lambda 表达式的调用者，即 forEach 循环
                print(it)
            }
            print(" done with implicit label")
        }

        fun foo4() {
            listOf(1, 2, 3, 4, 5).forEach(fun(value: Int) {
                if (value == 3) return  // 局部返回到匿名函数的调用者，即 forEach 循环
                print(value)
            })
            print(" done with anonymous function")
        }

        fun foo5() {
            run loop@{
                listOf(1, 2, 3, 4, 5).forEach {
                    if (it == 3) return@loop // 从传入 run 的 lambda 表达式非局部返回
                    print(it)
                }
            }
            print(" done with nested loop")
        }
    }
}