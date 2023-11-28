package com.vdian.android.lib.testforgradle.go_market

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import java.util.*


/**
 * @author yulun
 * @since 2023-11-15 10:40
 */
object MarketLaunchHelper {

    /*****OPPO******/
    private const val OPPO_PKG_MK_HEYTAP = "com.heytap.market" //Q之后的软件商店包名
    private const val OPPO_PKG_MK_OPPO = "com.oppo.market" //Q之前的软件商店包名
    private const val OPPO_COMMENT_DEEPLINK_PREFIX = "oaps://mk/developer/comment?pkg="
    private const val OPPO_SUPPORT_MK_VERSION = 84000 // 支持评论功能的软件商店版本
    /******OPPO*****/

    /*****VIVO******/
    private const val VIVO_PKG_MK_NAME = "com.bbk.appstore" //软件商店包名
    private const val VIVO_SUPPORT_MK_VERSION = 5020 // 支持评论功能的软件商店版本

    /*****VIVO******/


    //key=系统，value=商店包名
    private val marketPkgMap = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)

    init {
        marketPkgMap[Rom.BRAND_GOOGLE] = "com.android.vending" //Google Pixel-已测试

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            marketPkgMap[Rom.BRAND_OPPO] = OPPO_PKG_MK_OPPO//oppo-已测试
//            marketPkgMap[Rom.BRAND_ONEPLUS] = OPPO_PKG_MK_OPPO//一加-已测试
//            marketPkgMap[Rom.BRAND_REALME] = OPPO_PKG_MK_OPPO//realme-已测试
        } else {
            marketPkgMap[Rom.BRAND_OPPO] = OPPO_PKG_MK_HEYTAP//oppo-已测试
//            marketPkgMap[Rom.BRAND_ONEPLUS] = OPPO_PKG_MK_HEYTAP//一加-已测试
//            marketPkgMap[Rom.BRAND_REALME] = OPPO_PKG_MK_HEYTAP//realme-已测试
        }

        marketPkgMap[Rom.BRAND_XIAOMI] = "com.xiaomi.market"//小米-有问题，红米-已测试
        marketPkgMap[Rom.BRAND_REDMI] = "com.xiaomi.market"//小米-有问题，红米-已测试
        marketPkgMap[Rom.BRAND_MEIZU] = "com.meizu.mstore"//魅族-已测试
        marketPkgMap[Rom.BRAND_VIVO] = "com.bbk.appstore"//vivo-已测试
        marketPkgMap[Rom.BRAND_HUAWEI] = "com.huawei.appmarket"//华为-已测试，荣耀-已测试
        marketPkgMap[Rom.BRAND_HONOR] = "com.huawei.appmarket"//华为-已测试，荣耀-已测试
        marketPkgMap[Rom.BRAND_SAMSUNG] = "com.sec.android.app.samsungapps"//三星
        marketPkgMap[Rom.BRAND_LENOVO] = "com.lenovo.leos.appstore"//联想-已测试
        marketPkgMap[Rom.BRAND_LE_TV] = "com.letv.app.appstore"//乐视
    }

    /**
     * 唤起本app应用市场详情页
     */
    fun startMarket(context: Activity) {
        if (Rom.isEmui()) {
            startHuaweiMarket(context)
        } else if (Rom.isLeTv()) {
            if (launchLeTVStoreDetail(context)) {
                return
            } else {
                startDefaultMarket(context)
            }
        } else if (Rom.isSamsung()) {
            if (launchSamSungMarketIntent(context)) {
                return
            } else {
                startDefaultMarket(context)
            }
        }
        else if (Rom.isMiui()) {
            val packageName = "com.koudai.weidian.buyer"
            val marketPagName = marketPkgMap[Build.BRAND.uppercase()] ?: ""
            // &id=2882303761517271993
            launchOtherApp(context, Uri.parse("mimarket://details?id=$packageName"), marketPagName)
        }
        else {
            startDefaultMarket(context)
        }
    }

    /**
     * 默认方式启动应用市场
     */
    private fun startDefaultMarket(context: Activity) {
//            val packageName = context.packageName
        val packageName = "com.koudai.weidian.buyer"
        val marketPagName = marketPkgMap[Build.BRAND.uppercase()] ?: ""
        launchOtherApp(context, Uri.parse("market://details?id=$packageName"), marketPagName)
    }

    /**
     * 唤起本app应用市场详情页，同时拉起评论
     * 目前只有vivo oppo支持
     */
    fun startMarketEvaluate(context: Activity) {
        if (Rom.isOppo()) {
            startOppoMarketEvaluate(context)
        } else if (Rom.isVivo()) {
            startVivoMarketEvaluate(context)
        } else {
            startMarket(context)
        }
    }

    /**
     * 启动华为
     */
    private fun startHuaweiMarket(context: Context) {
        val intent = Intent("com.huawei.appmarket.intent.action.AppDetail")
        intent.setPackage("com.huawei.appmarket")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("APP_PACKAGENAME", "com.koudai.weidian.buyer")
        context.startActivity(intent)
    }

    private fun startVivoMarketEvaluate(context: Activity) {
        if (getVersionCode(context, VIVO_PKG_MK_NAME) >= VIVO_SUPPORT_MK_VERSION) {
            var url = "market://details?id=${"com.koudai.weidian.buyer"}&th_name=need_comment"
            var uri = Uri.parse(url);
            launchOtherApp(context, uri, VIVO_PKG_MK_NAME)
        } else {
            startMarket(context)
        }
    }


    /**
     * Oppo拉起评论页面。
     */
    private fun startOppoMarketEvaluate(context: Activity) {
        // 此处一定要传入调用方自己的包名，不能给其他应用拉起评论页。
        val url = OPPO_COMMENT_DEEPLINK_PREFIX + "com.koudai.weidian.buyer"
        // 优先判断heytap包
        if (getVersionCode(context, OPPO_PKG_MK_HEYTAP) >= OPPO_SUPPORT_MK_VERSION) {
            launchOtherApp(context, Uri.parse(url), OPPO_PKG_MK_HEYTAP)
        } else if (getVersionCode(context, OPPO_PKG_MK_OPPO) >= OPPO_SUPPORT_MK_VERSION) {
            launchOtherApp(context, Uri.parse(url), OPPO_PKG_MK_OPPO)
        } else {
            startMarket(context)
        }
    }


    /**
     * 获取目标app版本号~
     *
     * @param context
     * @param packageName
     * @return 返回版本号
     */
    private fun getVersionCode(context: Activity, packageName: String): Long {
        var versionCode: Long = -1
        try {
            val info =
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            if (info != null) {
                versionCode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return versionCode
    }

    private fun launchOtherApp(context: Activity, uri: Uri, targetPkgName: String): Boolean {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            if (!TextUtils.isEmpty(targetPkgName)) {
                intent.setPackage(targetPkgName)
            }
            intent.addCategory(Intent.CATEGORY_DEFAULT)
//            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = uri
            context.applicationContext.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun launchLeTVStoreDetail(context: Context): Boolean {
        try {
            var intent = Intent()
            intent.setClassName(
                "com.letv.app.appstore",
                "com.letv.app.appstore.appmodule.details.DetailsActivity"
            )
            intent.setAction("com.letv.app.appstore.appdetailactivity")
            intent.putExtra("packageName", context.packageName)
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 创建三星的Intent
     */
    private fun launchSamSungMarketIntent(
        context: Activity
    ): Boolean {
        try {
            val uri = Uri.parse(
                String.format(
                    "http://apps.samsung.com/appquery/appDetail.as?appId=%s",
                    context.packageName
                )
            )
            return launchOtherApp(context, uri, "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false

    }

}