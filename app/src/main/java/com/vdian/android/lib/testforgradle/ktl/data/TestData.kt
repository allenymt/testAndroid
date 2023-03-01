package com.vdian.android.lib.testforgradle.ktl.data


/**
 * @author yulun
 * @since 2022-01-24 11:39
 * data不能和open共存
 */
data class TestData(val name: String, val age: Int, val testData2: TestData2) {
}

//直接看编译后的字节码,可以看到，生成了一个public的final class，属性默认为private，有toString，equals，copy，get方法
//package com.vdian.android.lib.testforgradle.ktl;
//
//import kotlin.Metadata;
//import kotlin.jvm.internal.Intrinsics;
//
//@Metadata(d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0004\b\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007¢\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0010\u001a\u00020\u0005HÆ\u0003J\t\u0010\u0011\u001a\u00020\u0007HÆ\u0003J'\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007HÆ\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0016\u001a\u00020\u0005HÖ\u0001J\t\u0010\u0017\u001a\u00020\u0003HÖ\u0001R\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0006\u001a\u00020\u0007¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e¨\u0006\u0018"}, d2 = {"Lcom/vdian/android/lib/testforgradle/ktl/TestData;", "", "name", "", "age", "", "testData2", "Lcom/vdian/android/lib/testforgradle/ktl/TestData2;", "(Ljava/lang/String;ILcom/vdian/android/lib/testforgradle/ktl/TestData2;)V", "getAge", "()I", "getName", "()Ljava/lang/String;", "getTestData2", "()Lcom/vdian/android/lib/testforgradle/ktl/TestData2;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"}, k = 1, mv = {1, 5, 1}, xi = 48)
///* compiled from: TestData.kt */
//public final class TestData {
//    private final int age;
//    private final String name;
//    private final TestData2 testData2;
//
//    public static /* synthetic */ TestData copy$default(TestData testData, String str, int i, TestData2 testData2, int i2, Object obj) {
//        if ((i2 & 1) != 0) {
//            str = testData.name;
//        }
//        if ((i2 & 2) != 0) {
//            i = testData.age;
//        }
//        if ((i2 & 4) != 0) {
//            testData2 = testData.testData2;
//        }
//        return testData.copy(str, i, testData2);
//    }
//
//    public final String component1() {
//        return this.name;
//    }
//
//    public final int component2() {
//        return this.age;
//    }
//
//    public final TestData2 component3() {
//        return this.testData2;
//    }
//
//    public final TestData copy(String str, int i, TestData2 testData2) {
//        Intrinsics.checkNotNullParameter(str, "name");
//        Intrinsics.checkNotNullParameter(testData2, "testData2");
//        return new TestData(str, i, testData2);
//    }
//
//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//        if (!(obj instanceof TestData)) {
//            return false;
//        }
//        TestData testData = (TestData) obj;
//        return Intrinsics.areEqual(this.name, testData.name) && this.age == testData.age && Intrinsics.areEqual(this.testData2, testData.testData2);
//    }
//
//    public int hashCode() {
//        return (((this.name.hashCode() * 31) + Integer.hashCode(this.age)) * 31) + this.testData2.hashCode();
//    }
//
//    public String toString() {
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("TestData(name=");
//        stringBuilder.append(this.name);
//        stringBuilder.append(", age=");
//        stringBuilder.append(this.age);
//        stringBuilder.append(", testData2=");
//        stringBuilder.append(this.testData2);
//        stringBuilder.append(')');
//        return stringBuilder.toString();
//    }
//
//    public TestData(String name, int age, TestData2 testData2) {
//        Intrinsics.checkNotNullParameter(name, "name");
//        Intrinsics.checkNotNullParameter(testData2, "testData2");
//        this.name = name;
//        this.age = age;
//        this.testData2 = testData2;
//    }
//
//    public final int getAge() {
//        return this.age;
//    }
//
//    public final String getName() {
//        return this.name;
//    }
//
//    public final TestData2 getTestData2() {
//        return this.testData2;
//    }
//}