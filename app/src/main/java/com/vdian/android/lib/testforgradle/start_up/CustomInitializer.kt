package com.vdian.android.lib.testforgradle.start_up

import android.content.Context
import androidx.startup.Initializer

/**
 * @author yulun
 * @since 2022-07-12 20:36
 */

// 可以在合适的地方手动调用初始化
//AppInitializer.getInstance(this).initializeComponent(CustomInitializer::class.java)

class CustomInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        SdkSample().init(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(FirstInitializer::class.java)
    }
}

//先初始化的FirstInitializer
class FirstInitializer:Initializer<Unit> {
    override fun create(context: Context) {
        SdkSample().init(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}


