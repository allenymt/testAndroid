package com.vdian.android.lib.testforgradle.dataBinding

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.LayoutTestDatabindingBinding

class TestDateBindingActivity : AppCompatActivity() {
    private lateinit var userModel: UserModel
    private lateinit var mainBinding: LayoutTestDatabindingBinding
    private var count: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = DataBindingUtil.setContentView<LayoutTestDatabindingBinding>(
            this,
            R.layout.layout_test_databinding
        )
        //通过反射获取无参构造方法获得一个对象
        userModel = UserModel::class.java.newInstance()

        mainBinding.onViewClickHandler = OnViewClickHandler()

    }

    inner class OnViewClickHandler {
        fun userChangeClick() {
            mainBinding.isShow = true
            //模拟一下数据加载
            Handler(Looper.getMainLooper()).postDelayed({
                when (count) {
                    0 -> {
                        userModel.name = "小明"
                        userModel.sex = "男"
                    }
                    1 -> {
                        userModel.name = "李梅梅"
                        userModel.sex = "女"
                    }
                    else -> {
                        userModel.name = "张三"
                        userModel.sex = "男"
                    }
                }
                //数据绑定
                mainBinding.userModel = userModel
                mainBinding.isShow = false
                if (count == 2) count = 0 else count++
            }, 2000)
        }
    }
}