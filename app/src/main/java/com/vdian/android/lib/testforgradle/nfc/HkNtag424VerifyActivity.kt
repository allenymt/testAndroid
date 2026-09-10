package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.databinding.ActivityHkNtag424VerifyBinding
import java.util.concurrent.atomic.AtomicBoolean

class HkNtag424VerifyActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityHkNtag424VerifyBinding
    private var nfcAdapter: NfcAdapter? = null
    private val processing = AtomicBoolean(false)
    private val verifyClient = HkNtag424VerifyClient()
    @Volatile private var armed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHkNtag424VerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + 16.dp, bars.top + 12.dp, bars.right + 16.dp, bars.bottom + 12.dp)
            insets
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        binding.readAndVerify.setOnClickListener {
            armed = true
            binding.status.text = "已准备读卡验真，请贴卡并保持不动"
            binding.result.text = "将读取 File 02 动态 URL 并请求服务端验真"
        }
        binding.openNfcSettings.setOnClickListener { BuyerNFCUtil.Util.toOpenNfc(this) }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter
        when {
            adapter == null -> binding.status.text = "当前设备不支持 NFC"
            !adapter.isEnabled -> binding.status.text = "NFC 未开启，请先打开系统 NFC 设置"
            else -> adapter.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
    }

    override fun onPause() {
        nfcAdapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        if (!armed || !processing.compareAndSet(false, true)) return
        armed = false
        runOnUiThread { binding.status.text = "正在读取动态 NDEF 并请求服务端..." }
        val result = runCatching {
            val report = Ntag424DnaDemoClient.connect(tag).use { it.inspect() }
            check(report.ndefSettings?.sdmEnabled == true) { "File 02 尚未开启 SDM" }
            val url = checkNotNull(report.ndefUri) { "File 02 不包含 URI record" }.toString()
            check(url.hasOuterDynamicFields()) { "NDEF URL 顶层缺少有效的 e/c 动态参数" }
            url to verifyClient.verify(url)
        }
        runOnUiThread {
            result.onSuccess { (url, verification) ->
                binding.status.text = "服务器验真成功"
                binding.result.text = buildString {
                    appendLine("recordId: ${verification.recordId ?: "-"}")
                    appendLine("batchId: ${verification.batchId ?: "-"}")
                    appendLine("UID: ${verification.uid ?: "-"}")
                    appendLine("SN: ${verification.physicalSerialNo ?: "-"}")
                    appendLine("Counter: ${verification.counter ?: "-"}")
                    append("URL: $url")
                }
            }.onFailure { error ->
                Log.e(LOG_TAG, "read or server verification failed", error)
                binding.status.text = "验真失败"
                binding.result.text = error.message ?: error.javaClass.simpleName
            }
            processing.set(false)
        }
    }

    private fun String.hasOuterDynamicFields(): Boolean {
        val query = substringBefore('#').substringAfter('?', "")
        val values = query.split('&').mapNotNull { field ->
            val separator = field.indexOf('=')
            if (separator < 0) null else field.substring(0, separator).lowercase() to field.substring(separator + 1)
        }.groupBy({ it.first }, { it.second })
        val encrypted = values["e"]?.singleOrNull()
        val mac = values["c"]?.singleOrNull()
        return encrypted?.matches(Regex("[0-9A-Fa-f]{32}")) == true &&
            mac?.matches(Regex("[0-9A-Fa-f]{16}")) == true
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val LOG_TAG = "HkNtag424Verify"
    }
}
