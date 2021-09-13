package com.vdian.android.lib.testforgradle.rom

import android.os.Build
import android.os.Build.VERSION
import android.os.Process
import android.text.TextUtils
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.util.HashMap

/**
 * @author yulun
 * @sinice 2021-09-13 11:39
 */
class RomChecker {
    companion object{
        const val ROM_MIUI = "MIUI"
        const val ROM_EMUI = "EMUI"
        const val ROM_FLYME = "FLYME"
        const val ROM_OPPO = "OPPO"
        const val ROM_SMARTISAN = "SMARTISAN"
        const val ROM_VIVO = "VIVO"
        const val ROM_QIKU = "QIKU"
        const val ROM_SAMSUNG = "SAMSUNG"
        private const val TAG = "Rom"
        private const val KEY_VERSION_MIUI = "ro.miui.ui.version.name"
        private const val KEY_VERSION_EMUI = "ro.build.version.emui"
        private const val KEY_VERSION_OPPO = "ro.build.version.opporom"
        private const val KEY_VERSION_SMARTISAN = "ro.smartisan.version"
        private const val KEY_VERSION_VIVO = "ro.vivo.os.version"
        private var sName: String? = null
        private var sVersion: String? = null

        fun isEmui(): Boolean {
            return check("EMUI")
        }

        fun isMiui(): Boolean {
            return check("MIUI")
        }

        fun isVivo(): Boolean {
            return check("VIVO")
        }

        fun isOppo(): Boolean {
            return check("OPPO")
        }

        fun isFlyme(): Boolean {
            return check("FLYME")
        }

        fun is360(): Boolean {
            return check("QIKU") || check("360")
        }

        fun isSmartisan(): Boolean {
            return check("SMARTISAN")
        }

        fun isSamsung(): Boolean {
            return check("SAMSUNG")
        }

        fun getName(): String? {
            if (sName == null) {
                check("")
            }
            return sName
        }

        fun getVersion(): String? {
            if (sVersion == null) {
                check("")
            }
            return sVersion
        }

        fun check(rom: String): Boolean {
            return if (sName != null) {
                sName == rom
            } else {
                if (!TextUtils.isEmpty(getProp("ro.miui.ui.version.name").also { sVersion = it })) {
                    sName = "MIUI"
                } else if (!TextUtils.isEmpty(getProp("ro.build.version.emui").also {
                        sVersion = it
                    })) {
                    sName = "EMUI"
                } else if (!TextUtils.isEmpty(getProp("ro.build.version.opporom").also {
                        sVersion = it
                    })) {
                    sName = "OPPO"
                } else if (!TextUtils.isEmpty(getProp("ro.vivo.os.version").also {
                        sVersion = it
                    })) {
                    sName = "VIVO"
                } else if (!TextUtils.isEmpty(getProp("ro.smartisan.version").also {
                        sVersion = it
                    })) {
                    sName = "SMARTISAN"
                } else {
                    sVersion = Build.DISPLAY
                    if (sVersion?.toUpperCase()?.contains("FLYME") == true) {
                        sName = "FLYME"
                    } else {
                        sVersion = "unknown"
                        sName = Build.MANUFACTURER.toUpperCase()
                    }
                }
                sName == rom
            }
        }

        fun getProp(name: String): String? {
            var line: String? = null
            var input: BufferedReader? = null
            val var4: Any?
            try {
                val p = Runtime.getRuntime().exec("getprop $name")
                input = BufferedReader(InputStreamReader(p.inputStream), 1024)
                line = input.readLine()
                input.close()
                return line
            } catch (var14: IOException) {
                Log.e("Rom", "Unable to read prop $name", var14)
                var4 = null
            } finally {
                if (input != null) {
                    try {
                        input.close()
                    } catch (var13: IOException) {
                        var13.printStackTrace()
                    }
                }
            }
            return var4 as String?
        }

        fun commitRomVersion() : String{
            try {
                val romVersion = getProp("ro.build.display.id")
                val romVersion2 = getProp("sys.build.display.id")
                val romVersion3 = getProp("ro.build.software.version")
                val romVersion4 = getProp("ro.build.display.full_id")
                val brand = Build.BRAND
                val osVersion = VERSION.SDK_INT
                val sb = StringBuilder()
                sb.append("romVersion is ").append(romVersion).append("|romVersion2 is ")
                    .append(romVersion2).append("|romVersion3 is ").append(romVersion3)
                    .append("|romVersion4 is ").append(romVersion4).append("|").append(brand)
                    .append("|").append(osVersion)
                return sb.toString()
            } catch (var10: Exception) {
                var10.printStackTrace()
            }
            return ""
        }


        fun is64Process(): Boolean {
            var isCurrentProcess64 = false
            if (VERSION.SDK_INT >= 23) {
                isCurrentProcess64 = Process.is64Bit()
            }
            return isCurrentProcess64
        }

        fun is64Device(): Boolean {
            return if (VERSION.SDK_INT >= 21) {
                val abi64 = Build.SUPPORTED_64_BIT_ABIS
                abi64 != null && abi64.size > 0
            } else {
                false
            }
        }

        fun isOhos(): Boolean {
            var isOhos = false
            try {
                val cls = Class.forName("com.huawei.system.BuildEx")
                val method = cls.getMethod("getOsBrand")
                isOhos = "harmony" == method.invoke(cls)
            } catch (var3: Exception) {
            }
            return isOhos
        }
    }

}