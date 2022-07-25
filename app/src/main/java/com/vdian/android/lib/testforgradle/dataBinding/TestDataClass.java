package com.vdian.android.lib.testforgradle.dataBinding;

import kotlin.jvm.internal.Intrinsics;

/**
 * @author yulun
 * @since 2022-07-18 14:30
 */
class TestDataClass {
    public void test() {
        UserModel u1 = new UserModel();

        // 默认编译是报错的，因为没有无参构造函数
//        UserModel2 u2 = new UserModel2();

        // UserModel3变量都有默认值，所以有无参构造函数
        UserModel3 u3 = new UserModel3();
        Object newInstance = null;
        try {
            // 这个可以
            newInstance = UserModel.class.newInstance();

            // 这个可以
            Object newInstance3 = UserModel3.class.newInstance();

            // 这个不行
            Object newInstance2 = UserModel2.class.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        Intrinsics.checkNotNullExpressionValue(newInstance, "UserModel::class.java.newInstance()");
    }
}
