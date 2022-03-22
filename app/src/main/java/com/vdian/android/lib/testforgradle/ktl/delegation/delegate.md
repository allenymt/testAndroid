# 关于lazy的解释

## 首先，一旦某个属性被标记为lazy后，编译后就不是原有的属性值了
```kotlin
//源代码
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
```

编译后
```java
public final class TestDelegation {
    //可以看到属性都被改成Lazy了
    private final Lazy lazyProp$delegate = LazyKt__LazyJVMKt.lazy(TestDelegation$lazyProp$2.INSTANCE);
    private final Lazy lazyTestInt$delegate = LazyKt__LazyJVMKt.lazy(TestDelegation$lazyTestInt$2.INSTANCE);

    private final String getLazyProp() {
           TestDelegation$lazyTestInt$2就是个Function0，和Lambada一致，invoke方法做了初始化的操作
        return (String) this.lazyProp$delegate.getValue();
    }

    public final int getLazyTestInt() {
        return ((Number) this.lazyTestInt$delegate.getValue()).intValue();
    }

    public final void main() {
        System.out.println(getLazyProp());
        System.out.println(getLazyProp());
    }
}
```

```java
//只初始化一次怎么操作的呢? 从上面看到，LazyKt__LazyJVMKt.lazy主要是这个参与了过程
//由于默认配置是线程安全，所以会走到SynchronizedLazyImpl，接着看SynchronizedLazyImpl源码
class LazyKt__LazyJVMKt {
    public static final <T> Lazy<T> lazy(Function0<? extends T> initializer) {
        Intrinsics.checkNotNullParameter(initializer, "initializer");
        return new SynchronizedLazyImpl(initializer, null, 2, null);
    }

    public static final <T> Lazy<T> lazy(LazyThreadSafetyMode mode, Function0<? extends T> initializer) {
        Intrinsics.checkNotNullParameter(mode, "mode");
        Intrinsics.checkNotNullParameter(initializer, "initializer");
        int i = WhenMappings.$EnumSwitchMapping$0[mode.ordinal()];
        if (i == 1) {
        //默认走到了这里
            return new SynchronizedLazyImpl(initializer, null, 2, null);
        }
        if (i == 2) {
            return new SafePublicationLazyImpl(initializer);
        }
        if (i == 3) {
            return new UnsafeLazyImpl(initializer);
        }
        throw new NoWhenBranchMatchedException();
    }

    public static final <T> Lazy<T> lazy(Object lock, Function0<? extends T> initializer) {
        Intrinsics.checkNotNullParameter(initializer, "initializer");
        return new SynchronizedLazyImpl(initializer, lock);
    }
}
```


```java
// 源码其实很简单
final class SynchronizedLazyImpl<T> implements Lazy<T>, Serializable {
    private volatile Object _value;
    private Function0<? extends T> initializer;
    private final Object lock;

    public SynchronizedLazyImpl(Function0<? extends T> initializer, Object lock) {
        Intrinsics.checkNotNullParameter(initializer, "initializer");
        this.initializer = initializer;
        this._value = UNINITIALIZED_VALUE.INSTANCE;
        this.lock = lock != null ? lock : this;
    }

    public /* synthetic */ SynchronizedLazyImpl(Function0 function0, Object obj, int i, DefaultConstructorMarker defaultConstructorMarker) {
        if ((i & 2) != 0) {
            obj = null;
        }
        this(function0, obj);
    }
    
    //首次没有值的时候走初始化，其他就默认返回值，所以初始化块代码只有第一次会走
    public T getValue() {
        UNINITIALIZED_VALUE _v1 = this._value;
        if (_v1 != UNINITIALIZED_VALUE.INSTANCE) {
            return _v1;
        }
        T t;
        synchronized (this.lock) {
            t = this._value;
            if (t == UNINITIALIZED_VALUE.INSTANCE) {
                Object typedValue = this.initializer;
                Intrinsics.checkNotNull(typedValue);
                T typedValue2 = typedValue.invoke();
                this._value = typedValue2;
                this.initializer = (Function0) null;
                t = typedValue2;
            }
        }
        return t;
    }

    public boolean isInitialized() {
        return this._value != UNINITIALIZED_VALUE.INSTANCE;
    }

    public String toString() {
        return isInitialized() ? String.valueOf(getValue()) : "Lazy value not initialized yet.";
    }

    private final Object writeReplace() {
        return new InitializedLazyImpl(getValue());
    }
}
```