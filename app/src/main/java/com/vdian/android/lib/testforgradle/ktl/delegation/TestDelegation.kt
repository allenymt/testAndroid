package com.vdian.android.lib.testforgradle.ktl.delegation

import kotlin.reflect.KProperty

/**
 * @author yulun
 * @since 2022-01-25 14:01
 * 委托：https://www.kotlincn.net/docs/reference/delegation.html
 * 写的比较好的文章
 */
class TestDelegation {
    //延迟属性lazy
    private val lazyProp: String by lazy {
        println("Hello，第一次调用才会执行我！")
        "西哥！"
    }

    val lazyTestInt :Int by lazy {
        // do something
        1
    }

    // 打印lazyProp 3次，查看结果
    fun main() {
        //只有第一次调用的时候才会执行初始化模块，怎么做到的呢？ 分析太长，我们新启一个类来解释TestDelegationReadme
        println(lazyProp)
        println(lazyProp)
    }
}

// 委托里的关键字是by
// 先看类委托
interface DelegationBase {
    fun print()
}

class DelegationBaseImpl(val x: Int) : DelegationBase {
    override fun print() {
        print(x)
    }
}

//这里的定义是指什么？ 这语法看着好奇怪，说实话没发现实用点在哪？？
// 编译后呢？ 实际上就是DelegationDerived实现了DelegationBase接口，并且内部持有DelegationBase实例
class DelegationDerived(b: DelegationBase) : DelegationBase by b{
}

class DelegationDerived2: DelegationBase by DelegationBaseImpl(11){
}

fun main() {
    //测试类委托
    val b = DelegationBaseImpl(10)
    DelegationDerived(b).print()
    DelegationDerived2().print()

    //测试属性委托
    println(TestPropDelegate().prop)
    TestPropDelegate().prop = "Hello, Android技术杂货铺！"
}

//被委托类编译后的字节码
//public final class DelegationDerived implements DelegationBase {
//    private final /* synthetic */ DelegationBase $$delegate_0;
//
//    public DelegationDerived(DelegationBase b) {
//        Intrinsics.checkNotNullParameter(b, "b");
//        this.$$delegate_0 = b;
//    }
//
//    public void print() {
//        System.out.print("123123");
//    }
//}


//接着看属性委托
//委托属性语法: val/var <属性名>: <类型> by <表达式>
class TestPropDelegate {
    // 属性委托
    var prop: String by DelegateProp()
}

class DelegateProp {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "$thisRef, thank you for delegating '${property.name}' to me!"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("$value has been assigned to '${property.name}' in $thisRef.")
    }
}

//编译后的字节码，注意TestPropDelegate里的KProperty是反射创建的
//public final class TestPropDelegate {
//    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties = new KProperty[]{Reflection.mutableProperty1(new MutablePropertyReference1Impl(Reflection.getOrCreateKotlinClass(TestPropDelegate.class), "prop", "getProp()Ljava/lang/String;"))};
//    private final DelegateProp prop$delegate = new DelegateProp();
//
//    public final String getProp() {
//        return this.prop$delegate.getValue(this, $$delegatedProperties[0]);
//    }
//
//    public final void setProp(String <set-?>) {
//        Intrinsics.checkNotNullParameter(<set-?>, "<set-?>");
//        this.prop$delegate.setValue(this, $$delegatedProperties[0], <set-?>);prop$delegate就是DelegateProp
//    }
//}

//DelegateProp 没什么变化
//public final class DelegateProp {
//    public final String getValue(Object thisRef, KProperty<?> property) {
//        Intrinsics.checkNotNullParameter(property, "property");
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append(thisRef);
//        stringBuilder.append(", thank you for delegating '");
//        stringBuilder.append(property.getName());
//        stringBuilder.append("' to me!");
//        return stringBuilder.toString();
//    }
//
//    public final void setValue(Object thisRef, KProperty<?> property, String value) {
//        Intrinsics.checkNotNullParameter(property, "property");
//        Intrinsics.checkNotNullParameter(value, "value");
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append(value);
//        stringBuilder.append(" has been assigned to '");
//        stringBuilder.append(property.getName());
//        stringBuilder.append("' in ");
//        stringBuilder.append(thisRef);
//        stringBuilder.append('.');
//        System.out.println(stringBuilder.toString());
//    }
//}