package com.vdian.android.lib.testforgradle.nfc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Shows the current configuration-to-card mapping, not a separate write history. */
internal class HkNfcConfigPanel(
    private val activity: AppCompatActivity,
    container: LinearLayout,
    private val assetName: String,
    private val selectedIndex: () -> Int?
) {
    private val file = HkNfcConfigFile(activity, assetName)
    private val summary = TextView(activity).apply { setTextIsSelectable(true) }
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EXPORT_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            data?.data?.let { uri ->
                perform {
                    val json = file.read()
                    checkNotNull(activity.contentResolver.openOutputStream(uri, "wt")) {
                        "无法打开导出文件"
                    }.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                    toast("JSON 已导出")
                }
            }
        }
    }

    init {
        container.addView(summary)
        container.addView(Button(activity).apply {
            text = "查看完整 JSON"
            setOnClickListener { perform { showJson(file.read()) } }
        })
        container.addView(Button(activity).apply {
            text = "复制完整 JSON"
            setOnClickListener {
                confirmDisclosure {
                    perform {
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(assetName, file.read()))
                        toast("JSON 已复制")
                    }
                }
            }
        })
        container.addView(Button(activity).apply {
            text = "导出 JSON 文件"
            setOnClickListener {
                confirmDisclosure {
                    perform {
                        activity.startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                            putExtra(Intent.EXTRA_TITLE, assetName)
                        }, EXPORT_REQUEST)
                    }
                }
            }
        })
    }

    fun refresh() {
        val index = selectedIndex()
        summary.text = if (index == null) "请选择配置以查看已写卡 UID" else runCatching {
            val ids = HkNfcConfigJson.rfidsAt(file.read(), index)
            "配置 ${index + 1} · 已写入 ${ids.size} 张卡\n" + ids.joinToString("\n").ifEmpty { "未写入" }
        }.getOrElse { "读取 rfid 失败：${it.message}" }
    }

    private fun showJson(json: String) {
        val content = TextView(activity).apply {
            text = json
            setPadding(24, 16, 24, 16)
            setTextIsSelectable(true)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        AlertDialog.Builder(activity)
            .setTitle(assetName)
            .setView(ScrollView(activity).apply { addView(content) })
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun confirmDisclosure(action: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("导出配置数据")
            .setMessage("完整 JSON 包含写卡配置、UID，424 配置还包含密钥。请仅交付给获授权人员。")
            .setNegativeButton("取消", null)
            .setPositiveButton("继续") { _, _ -> action() }
            .show()
    }

    private fun perform(action: () -> Unit) {
        runCatching(action).onFailure { toast(it.message ?: "操作失败") }
    }

    private fun toast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_LONG).show()

    companion object {
        private const val EXPORT_REQUEST = 21542
    }
}
