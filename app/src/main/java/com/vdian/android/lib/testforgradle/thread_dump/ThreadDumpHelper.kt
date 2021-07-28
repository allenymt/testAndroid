package com.vdian.android.lib.testforgradle.thread_dump

import android.text.TextUtils

/**
 * @author yulun
 * @sinice 2021-07-20 16:44
 */
class ThreadDumpHelper {
    fun dumpThreadInfo(): HashMap<String, Any>? {
        val threadMap: Map<Thread?, Array<StackTraceElement?>?>? = Thread.getAllStackTraces()
        // read current all thread info
        if (threadMap?.isEmpty()!!)
            return null
        var nameCountMap = HashMap<String, Any>()
        threadMap?.forEach<Thread?, Array<StackTraceElement?>?> {
            try {
                var charThreadName = parseThreadName(it.key?.name ?: "")
                if (!TextUtils.isEmpty(charThreadName)){
                    if (nameCountMap.containsKey(charThreadName)) {
                        var count = nameCountMap[charThreadName] as Int
                        if (count == null) {
                            count = 1
                        }
                        count++
                        count?.let { nameCountMap.put(charThreadName, count) }
                    } else {
                        nameCountMap[charThreadName] = 1
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return nameCountMap
    }

    private fun parseThreadName(threadName: String): String {
        var resultName = threadName
        return resultName
    }

}