package com.vdian.android.lib.testforgradle.nfc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.databinding.ActivityHkNtag424VerifyBinding
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors

class HkNtag424VerifyActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityHkNtag424VerifyBinding
    private var nfcAdapter: NfcAdapter? = null
    private val processing = AtomicBoolean(false)
    private val verifyClient = HkNtag424VerifyClient()
    private val networkExecutor = Executors.newSingleThreadExecutor()
    private var canVerify = false
    @Volatile private var armed = false
    @Volatile private var latestUrl: String? = null

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
        binding.ndefUrl.movementMethod = ScrollingMovementMethod()
        binding.copyUrl.isEnabled = false
        binding.readCard.setOnClickListener {
            if (processing.get()) return@setOnClickListener
            latestUrl = null
            canVerify = false
            binding.verifyUrl.isEnabled = false
            binding.cardInfo.text = "UID: 等待读卡"
            binding.ndefUrl.text = "等待读卡"
            binding.copyUrl.isEnabled = false
            binding.status.text = "已准备读取，请贴卡并保持不动"
            binding.result.text = "读取只获取卡片数据，不请求服务端"
            armed = true
        }
        binding.verifyUrl.setOnClickListener { verifyLatestUrl() }
        binding.copyUrl.setOnClickListener { copyLatestUrl() }
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
        armed = false
        nfcAdapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        if (!armed || !processing.compareAndSet(false, true)) return
        armed = false
        runOnUiThread {
            binding.readCard.isEnabled = false
            binding.verifyUrl.isEnabled = false
            binding.status.text = "正在读取动态 NDEF..."
        }
        val result = runCatching {
            val report = Ntag424DnaDemoClient.connect(tag).use { it.inspect() }
            val url = report.ndefUri?.toString()
            val records = if (report.version.uid?.any { it != 0.toByte() } == true) emptyList() else {
                runCatching { HkNtag424RecordRepository(this).load() }.getOrDefault(emptyList())
            }
            Triple(report, url, HkNtag424ReadUid.resolve(report.version.uid, url, records))
        }
        runOnUiThread {
            if (isDestroyed) return@runOnUiThread
            result.onSuccess { (report, url, uid) ->
                latestUrl = url
                canVerify = report.ndefSettings?.sdmEnabled == true && url?.hasOuterDynamicFields() == true
                binding.ndefUrl.text = url ?: "未发现 URI record"
                binding.copyUrl.isEnabled = url != null
                binding.cardInfo.text = buildString {
                    appendLine("UID: ${uid ?: "无法解析真实 UID（Random UID 模式且本地密钥不匹配）"}")
                    appendLine("射频 ID: ${tag.id.joinToString(":") { "%02X".format(it) }}")
                    append("SDM: ${if (report.ndefSettings?.sdmEnabled == true) "已开启" else "未开启或不可读取"}")
                }
                binding.status.text = "读卡完成，尚未请求服务端"
                binding.result.text = if (canVerify) "请点击第 2 步验真，无需再次贴卡" else
                    "当前 NDEF 未开启 SDM 或缺少有效的外层 e/c，无法验真"
            }.onFailure { error ->
                Log.e(LOG_TAG, "card read failed", error)
                binding.status.text = "读卡失败"
                binding.result.text = error.message ?: error.javaClass.simpleName
            }
            processing.set(false)
            binding.readCard.isEnabled = true
            binding.verifyUrl.isEnabled = canVerify
        }
    }

    private fun verifyLatestUrl() {
        val url = latestUrl ?: return
        if (!canVerify || !processing.compareAndSet(false, true)) return
        armed = false
        binding.readCard.isEnabled = false
        binding.verifyUrl.isEnabled = false
        binding.status.text = "正在使用已读 URL 请求服务端验真..."
        networkExecutor.execute {
            val result = runCatching { verifyClient.verify(url) }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                result.onSuccess { verification ->
                    binding.status.text = "服务器验真成功"
                    binding.result.text = "UID: ${verification.uid ?: "-"}\nCounter: ${verification.counter ?: "-"}"
                }.onFailure { error ->
                    Log.e(LOG_TAG, "server verification failed", error)
                    binding.status.text = "服务器验真失败"
                    binding.result.text = error.message ?: error.javaClass.simpleName
                }
                processing.set(false)
                binding.readCard.isEnabled = true
                binding.verifyUrl.isEnabled = canVerify
            }
        }
    }

    override fun onDestroy() {
        networkExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun copyLatestUrl() {
        val url = latestUrl ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NTAG 424 SDM URL", url))
        Toast.makeText(this, "URL 已复制", Toast.LENGTH_SHORT).show()
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
