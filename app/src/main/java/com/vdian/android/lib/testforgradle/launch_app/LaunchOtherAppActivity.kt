package com.vdian.android.lib.testforgradle.launch_app

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
//            call3()
//            toTaoBao("https://shop102469196.m.taobao.com/?weexShopTab=shopindexbar&weexShopSubTab=shopindex&sourceType=other&suid=d8d022b1-0586-44a7-9079-6a1389342625&shareUniqueId=25050647876&ut_sk=1.ZVWtJ9uKU30DALSlId1rBk9k_21646297_1704789040519.Copy.shop&un=2af9581806d803d68bd7bab98ffeb263&share_crt_v=1&un_site=0&spm=a2159r.13376460.0.0&sp_abtk=common_shop_commonInfo&sp_tk=5L2g5Y675aW95Zyw5Liq5bm05LiN5Li65Y%2Bv6KaB5LmL&cpp=1&shareurl=true&short_name=h.5LIkARIDg93uQ2U&bxsign=scd88Ct8R0tTzJdHtXwHmmcZUttKnAoUEnlOPcXm36VWts_9YtyhXbN6F3YNZl43r08PQkoa3AuV23rd2Zqd_gVraQIVtDK_xHqUg1PZc6GnnGGq-uF8KMq0_eqgT5fLLBg")
             toTaoBao("taobao://shop.m.taobao.com/shop/shop_index.htm?shop_id=145892083")
//            toTaoBao("taobao://legendsandy.m.tmall.com/")

//            toTaoBao("taobao://shop.m.taobao.com/shop/shop_index.htm?shop_id=185441148")
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

            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt")
//            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=4420472834")
//            intent.data =
//                Uri.parse("https://h5.weidian.com/m/weidian-buyer/cross-app/index.html?ckey=CK1370428950809&redirectUrl=weidianbuyer://wdb/follow?tabId=1000001&groupId=1?h5=1&ckey=CK1370428950809")

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    fun call3() {
//        var intent = packageManager.getLaunchIntentForPackage("com.taobao.taobao")
//        if (intent == null) {
//            intent = Intent()
//        }
       var intent = Intent()
        if (intent != null) {
            /**
             * https://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt
            weidianbuyer://wdb/goods_detail?itemId=2675728831&wfr=hongbaowt&ffr=hongbaowt
            https://wdb-applink.weidian.com/?targetUrl=https%3A%2F%2Fweidian.com%2Fitem.html%3FitemID%3D2675728831%26wfr%3Dhongbaowt%26ffr%3Dhongbaowt
             */

            intent.data = Uri.parse("taobao://shop.m.taobao.com/shop/shop_index.htm?shop_id=185441148")
//            intent.data = Uri.parse("taobao://shop.m.taobao.com/shop/shop_index.htm?shop_id=12000516")
//            intent.data = Uri.parse("weidianbuyer://wdb/goods_detail?itemId=4420472834")
//            intent.data =
//                Uri.parse("https://h5.weidian.com/m/weidian-buyer/cross-app/index.html?ckey=CK1370428950809&redirectUrl=weidianbuyer://wdb/follow?tabId=1000001&groupId=1?h5=1&ckey=CK1370428950809")

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun toTaoBao(url: String) {

        intent = Intent()
        intent.data = Uri.parse(url)
//        intent.action = "android.intent.action.VIEW"
        intent.setPackage("com.taobao.taobao")
        val resolveInfo: ResolveInfo? =
            getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        startActivity(intent)

//        val intent = Intent()
//        intent.action = "android.intent.action.VIEW"
//        intent.setPackage("com.taobao.taobao")
//        val uri = Uri.parse(url)
//        intent.data = uri
////        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(intent)
        /**第二种方式
         * String id = getUrlParam(url,"id");
         * String taobao_url= "taobao://item.taobao.com/item.htm?id="+id+"";
         * intent = new Intent(Intent.ACTION_VIEW, Uri.parse(taobao_url));
         * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         * startActivity(intent);  */
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