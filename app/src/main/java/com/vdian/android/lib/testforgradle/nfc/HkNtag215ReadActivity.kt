package com.vdian.android.lib.testforgradle.nfc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.databinding.ActivityHkNtag215ReadBinding
import java.util.concurrent.atomic.AtomicBoolean

class HkNtag215ReadActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityHkNtag215ReadBinding
    private var adapter: NfcAdapter? = null
    private val armed = AtomicBoolean(false)
    private val processing = AtomicBoolean(false)
    private var latestUrls: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHkNtag215ReadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + 16.dp, bars.top + 12.dp, bars.right + 16.dp, bars.bottom + 12.dp)
            insets
        }
        adapter = NfcAdapter.getDefaultAdapter(this)
        binding.copyUrl.isEnabled = false
        binding.readCard.setOnClickListener {
            latestUrls = emptyList()
            binding.copyUrl.isEnabled = false
            binding.result.text = "等待读取 URL、包名及卡片信息"
            binding.status.text = "已准备读取，请贴 NTAG215 并保持不动"
            armed.set(true)
        }
        binding.copyUrl.setOnClickListener {
            if (latestUrls.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("NTAG215 URL", latestUrls.joinToString("\n")))
                Toast.makeText(this, "URL 已复制", Toast.LENGTH_SHORT).show()
            }
        }
        binding.openNfcSettings.setOnClickListener { BuyerNFCUtil.Util.toOpenNfc(this) }
    }

    override fun onResume() {
        super.onResume()
        val nfc = adapter
        when {
            nfc == null -> binding.status.text = "当前设备不支持 NFC"
            !nfc.isEnabled -> binding.status.text = "请先开启 NFC"
            else -> nfc.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, null)
        }
    }

    override fun onPause() {
        armed.set(false)
        adapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        if (!processing.compareAndSet(false, true)) return
        if (!armed.getAndSet(false)) {
            processing.set(false)
            return
        }
        runOnUiThread {
            binding.readCard.isEnabled = false
            binding.status.text = "正在读取 NTAG215"
        }
        val result = runCatching { HkNtag215Reader.read(tag) }
        runOnUiThread {
            result.onSuccess { report ->
                latestUrls = report.urls
                binding.copyUrl.isEnabled = latestUrls.isNotEmpty()
                binding.status.text = if (report.message == null) "读取成功，NDEF 为空" else "NTAG215 读取成功"
                binding.result.text = buildString {
                    appendLine("UID: ${report.uid}")
                    appendLine("NDEF 容量: ${report.capacity} bytes")
                    appendLine("可写: ${if (report.writable) "是" else "否"}")
                    appendLine("记录数: ${report.message?.records?.size ?: 0}")
                    appendLine("\nURL:\n${report.urls.joinToString("\n").ifEmpty { "未发现 URI 记录" }}")
                    append("\n包名:\n${report.packages.joinToString("\n").ifEmpty { "未发现 Android 包名记录" }}")
                }
            }.onFailure { error ->
                Log.e("HkNtag215Read", "NDEF read failed", error)
                latestUrls = emptyList()
                binding.copyUrl.isEnabled = false
                binding.status.text = "读取失败"
                binding.result.text = error.message ?: error.javaClass.simpleName
            }
            processing.set(false)
            binding.readCard.isEnabled = true
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
