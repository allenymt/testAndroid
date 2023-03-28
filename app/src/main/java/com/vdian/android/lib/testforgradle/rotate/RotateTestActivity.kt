package com.vdian.android.lib.testforgradle.rotate

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.databinding.ActivityRotateTestBinding

class RotateTestActivity : AppCompatActivity() {

    val TAG = "RotateTestActivity"

    private lateinit var _rotateTestViewBinding: ActivityRotateTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _rotateTestViewBinding = ActivityRotateTestBinding.inflate(layoutInflater)
        setContentView(_rotateTestViewBinding.root)
        _rotateTestViewBinding.etName.setOnEditorActionListener(object :
            TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                log("RotateTestActivity-onEditorAction-${event}")
                return false
            }
        })
        _rotateTestViewBinding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                log("RotateTestActivity-onTextChanged-${s}")
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        log("RotateTestActivity-onCreate-${savedInstanceState}")
    }

    override fun onStart() {
        super.onStart()
        log("RotateTestActivity-onStart")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        log("RotateTestActivity-onRestoreInstanceState-$savedInstanceState")
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        log("RotateTestActivity-onPostCreate")
    }

    override fun onResume() {
        super.onResume()
        log("RotateTestActivity-onResume")
    }

    override fun onPostResume() {
        super.onPostResume()
        log("RotateTestActivity-onPostResume")
    }

    override fun onPause() {
        super.onPause()
        log("RotateTestActivity-onPause")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        log("RotateTestActivity-onSaveInstanceState-$outState")
    }

    override fun onStop() {
        super.onStop()
        log("RotateTestActivity-onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("RotateTestActivity-onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        log("RotateTestActivity-onRestart")
    }

    // manifest里必现注册android:configChanges="orientation|screenSize"
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        log("RotateTestActivity-onConfigurationChanged-$newConfig")
    }

    private fun log(msg: String) {
        android.util.Log.i(TAG, "$msg")
    }

    fun showDialog(view: View) {
        var dialogFragment = RotateDialogFragment.newInstance("param1", "param2")
        // 官方是推荐 不保存状态的Fragment+ViewModel来处理横竖屏的问题
        dialogFragment.retainInstance = true
        dialogFragment.show(supportFragmentManager, "dialog")
    }
}