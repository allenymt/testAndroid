package com.vdian.android.lib.testforgradle.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.applink.AppLinkTestDomainActivity
import com.vdian.android.lib.testforgradle.rom.RomCheck
import com.vdian.android.lib.testforgradle.single.TestStaticInnerSingle
import java.net.URI
import java.net.URLDecoder

/**
 * @author yulun
 * @since 2022-07-25 16:58
 */
class Util {
    object Util {
        /**
         * 判断是不是鸿蒙系统
         *
         * @return
         */
        fun isOhos(): Boolean {
            return RomCheck.isOhos()
        }

        fun getStatusBarHeight(context: Context): Int {
            // 获得状态栏高度
            val resourceId =
                context.resources.getIdentifier("status_bar_height", "dimen", "android")
            return context.resources.getDimensionPixelSize(resourceId)
        }


        fun testTryCache(mHandler: Handler) {
            try {
                mHandler.postDelayed(Runnable { throw NullPointerException("123123") }, 500)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        fun testDrawableSize(context: Context) {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_launcher_test_drawable,
                options
            )
            println("bitmap test  w is : " + bitmap.width + " h is " + bitmap.height + "  config" + bitmap.config.name + " mDensity:" + bitmap.density)
            println("bitmap test byte_count: " + bitmap.byteCount)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) println("bitmap test  allocation_count: " + bitmap.allocationByteCount)
        }

        fun testAssetsSize(context: Context) {
            val assetManager: AssetManager = context.resources.assets
            try {
                val inputStream = assetManager.open("ic_launcher_test_assets.png")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                println("bitmap test  w is : " + bitmap.width + " h is " + bitmap.height + "  config" + bitmap.config.name + " mDensity:" + bitmap.density)
                println("bitmap test byte_count: " + bitmap.byteCount)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) println("bitmap test  allocation_count: " + bitmap.allocationByteCount)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        fun testPush() {
            //默认添加上scheme
            var p =
                "https://h5.weidian.com/m/koubei/content-detail.html?authorId=295567927&feedId=234211848&0603@spoor=1011.70976.0.2315.videoFeed-295567927-toc-234211848%7Cfocus%26a42281a0-7d26-4cf9-a54a-07a2f5091677"
            try {
                val scheme = URI.create(p.trim { it <= ' ' }).scheme
                if (scheme == null) {
                    p = "weidianbuyer://$p"
                }
            } catch (e: java.lang.Exception) {
//            LogUtil.getLogger().w(e.getMessage(), e);
                p = "weidianbuyer://$p"
            }
        }

        fun testSegment() {
            try {
                val dynamicRouteBuilder = Uri.Builder()
                val intent = Intent()
                val uri =
                    Uri.parse("https://h5.weidian.com/m/weidian/shop-new.html#?shopId=1351700016&referrer=push&0303@spoor=1011.64704.0.1178.newItemFeed")
                val bundle = Bundle()
                val query = uri.encodedQuery
                var params = arrayOf<String>()
                if (!TextUtils.isEmpty(query)) {
                    params = query!!.split("&").toTypedArray()
                }
                if (params.isNotEmpty()) {
                    for (param in params) {
                        val keyValue = param.split(Regex("="), 2)
                        val name = keyValue[0]
                        var value = ""
                        if (keyValue.size > 1) {
                            value = keyValue[1]
                        }
                        try {
                            bundle.putString(name, URLDecoder.decode(value, "UTF-8"))
                            dynamicRouteBuilder.appendQueryParameter(
                                name,
                                URLDecoder.decode(value, "UTF-8")
                            )
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                    intent.putExtras(bundle)
                }
                val segment = uri.encodedFragment
                if (!TextUtils.isEmpty(segment)) {
                    dynamicRouteBuilder.encodedFragment(segment)
                    if (segment!!.contains("?")) {
                        var seParams = arrayOf<String>()
                        val segmentParam =
                            segment.substring(segment.indexOf("?") + 1, segment.length)
                        seParams = segmentParam.split("&").toTypedArray()
                        if (seParams.isNotEmpty()) {
                            for (param in seParams) {
                                val keyValue = param.split(Regex("="), 2).toTypedArray()
                                val name = keyValue[0]
                                var value = ""
                                if (keyValue.size > 1) {
                                    value = keyValue[1]
                                }
                                bundle.putString(name, URLDecoder.decode(value, "UTF-8"))
                            }
                            intent.putExtras(bundle)
                        }
                    }
                }
                intent.data = dynamicRouteBuilder.build()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        fun toLink(v: View?) {
            TestStaticInnerSingle.getInstance()
            v?.context?.startActivity(Intent(v.context, AppLinkTestDomainActivity::class.java))
        }


        fun lengthOfLongestSubstring(s: String): Int {
            var leftStart = 0
            var rightEnd = 0
            val maxLength = 0
            //        HashSet<Character> pool = new HashSet<>();
            val poolnew = ByteArray(256)
            var maxValidLength = 0
            var currentLength = 0
            val len = s.length
            while (rightEnd < len) {
                val character = s[rightEnd]
                val hash = character.code - '0'.code
                if (poolnew[hash] == 1.toByte()) {
                    while (character != s[leftStart]) {
                        poolnew[s[leftStart].code] = 0
                        leftStart++
                    }
                    leftStart++
                } else {
                    currentLength = rightEnd - leftStart + 1
                    if (maxValidLength < currentLength) {
                        maxValidLength = currentLength
                    }
                    poolnew[hash] = 1
                }
                rightEnd++
            }
            return maxLength
        }

        fun hideSystemUi(context: Activity) {
            //隐藏虚拟按键，并且全屏
            if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
                val v = context.window.decorView
                v.systemUiVisibility = View.GONE
            } else if (Build.VERSION.SDK_INT >= 19) {
                //for new api versions.
                val decorView = context.window.decorView
                val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
                decorView.systemUiVisibility = uiOptions
                context.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            }
        }

    }
}