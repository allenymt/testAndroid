package com.vdian.android.lib.testforgradle.launch_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.vdian.android.lib.testforgradle.R


/**
 * @author yulun
 * @sinice 2021-04-14 11:08
 */
class LaunchOtherAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_app)
        findViewById<View>(R.id.test).setOnClickListener {
            call1()
        }
    }


    fun call1(){
        var intent = packageManager.getLaunchIntentForPackage("com.koudai.weidian.buyer")
        if (intent == null){
            intent = Intent()
        }
        if (intent != null) {
            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            var bundle = Bundle()
            bundle.putString("product_id","3857967270")
            bundle.putString("wfr","hongbaowt")
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    fun call2(){
        var intent = packageManager.getLaunchIntentForPackage("com.koudai.weidian.buyer")
        if (intent == null){
            intent = Intent()
        }
        if (intent != null) {
            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=3857967270&wfr=hongbaowt&ffr=hongbaowt")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}