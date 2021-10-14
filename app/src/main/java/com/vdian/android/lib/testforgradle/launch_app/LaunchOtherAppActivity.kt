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
            call2()
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
            /**
             * https://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt
            weidianbuyer://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt
            https://wdb-applink.weidian.com/?targetUrl=https%3A%2F%2Fweidian.com%2Fitem.html%3FitemID%3D2675728831%26wfr%3Dhongbaowt%26ffr%3Dhongbaowt
             */

//            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt")
            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=4420472834&wfr=hongbaowt&ffr=hongbaowt&adsk=shop;12533892;cpc;outer_bytedance;1633991388376_7WTVUExkcxobqtme_9JemRxttyX8KdcKlOU8J4atHWI2AMITrucKyAQAbo0VnkXpJhN0L3-1aCmhK1v0rVRKbY_EX-1MLRIfWnye8A4l7LbWxzFhuLmt89mQ_pvkhI284aGLtVnUUQL7jxazPMCc2YnDW9hudUqjYKlrxgCf0RTV2FHSs9LRhQiL5gQ91qTin3KL20QQ1g7HGHwLPMdVGBJ-aVoUfTo-d6ju4EFxoidgxwcszIHs3C7xbqZFyoBQlPEjjb_tpRa1DLLve_a941df7f314acb7b145989dd0ee705ede7de47e4679c22f16d45d32a5e1c0513;;1;;1612834184&adid=1713327140526123&cid=1713327569212468&csite=900000000&union_site=2033739452&requestid=ef753cc0-a427-4a61-99dd-916016b9b70fu8412&callback_url=http%3A%2F%2Fad.toutiao.com%2Ftrack%2Factivate%2F%3Fcallback%3DCKuw4fywyIUDELSolsmyyIUDGI2LoNi69EUgvrabnIcCKAAwDDjclu_CA0IpZWY3NTNjYzAtYTQyNy00YTYxLTk5ZGQtOTE2MDE2YjliNzBmdTg0MTJIgNKTrQNQAJABAA%26os%3D0%26muid%3Dd64bc3753df361f6a937b46f2b5c029d&backurl=__back_url__")


            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}