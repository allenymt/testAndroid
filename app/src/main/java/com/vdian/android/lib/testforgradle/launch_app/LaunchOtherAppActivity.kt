package com.vdian.android.lib.testforgradle.launch_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
            call2()
        }

        findViewById<View>(R.id.call_market).setOnClickListener {
            callLaunchMarket()
        }
    }


    fun call1() {
        var intent = packageManager.getLaunchIntentForPackage("com.koudai.weidian.buyer")
        if (intent == null) {
            intent = Intent()
        }
        if (intent != null) {
            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            var bundle = Bundle()
            bundle.putString("product_id", "3857967270")
            bundle.putString("wfr", "hongbaowt")
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }

    fun call2() {
        var intent = packageManager.getLaunchIntentForPackage("com.koudai.weidian.buyer")
        if (intent == null) {
            intent = Intent()
        }
        if (intent != null) {
            /**
             * https://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt
            weidianbuyer://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt
            https://wdb-applink.weidian.com/?targetUrl=https%3A%2F%2Fweidian.com%2Fitem.html%3FitemID%3D2675728831%26wfr%3Dhongbaowt%26ffr%3Dhongbaowt
             */

//            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt")
//            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=4420472834")
            intent.data =
                Uri.parse("https://h5.weidian.com/m/weidian-buyer/cross-app/index.html?ckey=CK1370428950809&redirectUrl=weidianbuyer://wdb/follow?tabId=1000001&groupId=1?h5=1&ckey=CK1370428950809")

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }


    fun callLaunchMarket() {
        var intent = Intent(Intent.ACTION_VIEW,null)
        intent.data = Uri.parse("market://details?id=com.koudai.weidian.buyer")
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            var bundle = Bundle()
//            bundle.putString("product_id","3857967270")
//            bundle.putString("wfr","hongbaowt")
//            intent.putExtras(bundle)
        startActivity(intent)
    }
}