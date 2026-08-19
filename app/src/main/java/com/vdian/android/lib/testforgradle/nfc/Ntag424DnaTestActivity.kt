package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivityNtag424DnaTestBinding
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Ntag424DnaTestActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityNtag424DnaTestBinding
    private var nfcAdapter: NfcAdapter? = null
    private val processingTag = AtomicBoolean(false)
    private val pendingOperation = AtomicReference(Operation.INSPECT)
    @Volatile private var pendingWriteRequest: WriteRequest? = null
    @Volatile private var pendingFile03Text: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOG_TAG, "$LOG_PREFIX page created, restored=${savedInstanceState != null}")
        binding = ActivityNtag424DnaTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        binding.openNfcSettings.setOnClickListener { BuyerNFCUtil.Util.toOpenNfc(this) }
        binding.clearNtag424Result.setOnClickListener {
            Log.i(LOG_TAG, "$LOG_PREFIX result cleared by user")
            binding.ntag424Result.text = getString(R.string.ntag424_waiting_for_tag)
        }
        binding.writeNtag424Ndef.setOnClickListener { confirmBusinessNdefWrite() }
        binding.enableSdmZeroKey.setOnClickListener { confirmZeroKeySdmConfiguration() }
        binding.disableSdmZeroKey.setOnClickListener { confirmDisableSdm() }
        binding.enableNtag424WriteProtection.setOnClickListener {
            confirmWriteProtection(protected = true)
        }
        binding.disableNtag424WriteProtection.setOnClickListener {
            confirmWriteProtection(protected = false)
        }
        binding.writeNtag424File03.setOnClickListener { confirmFile03Write() }
        binding.readNtag424File03.setOnClickListener { armFile03Read() }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter
        when {
            adapter == null -> {
                Log.w(LOG_TAG, "$LOG_PREFIX reader unavailable: device does not support NFC")
                binding.ntag424Status.text = "当前设备不支持 NFC"
            }
            !adapter.isEnabled -> {
                Log.w(LOG_TAG, "$LOG_PREFIX reader unavailable: NFC is disabled")
                binding.ntag424Status.text = "NFC 未开启，请先打开系统 NFC 设置"
            }
            else -> {
                binding.ntag424Status.text = "NFC 已开启，等待 NTAG 424 DNA 卡片"
                adapter.enableReaderMode(
                    this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null
                )
                Log.i(LOG_TAG, "$LOG_PREFIX reader mode enabled, technologies=NFC_A, skipNdefCheck=true")
            }
        }
    }

    override fun onPause() {
        nfcAdapter?.disableReaderMode(this)
        Log.i(LOG_TAG, "$LOG_PREFIX reader mode disabled")
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        if (!processingTag.compareAndSet(false, true)) {
            Log.w(LOG_TAG, "$LOG_PREFIX ignored tag while another inspection is running")
            return
        }
        val startedAt = SystemClock.elapsedRealtime()
        val operation = pendingOperation.getAndSet(Operation.INSPECT) ?: Operation.INSPECT
        val writeRequest = pendingWriteRequest.also { pendingWriteRequest = null }
        val file03Text = pendingFile03Text.also { pendingFile03Text = null }
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX tag discovered, operation=$operation, " +
                "rfUid=${tag.id.toHex(":")}, tech=${tag.techList.joinToString()}"
        )
        runOnUiThread {
            binding.ntag424Status.text = when (operation) {
                Operation.WRITE_NDEF -> "正在写入 URL + pkg，请勿移开卡片..."
                Operation.ENABLE_SDM -> "正在使用全零 Key 0 配置 SDM，请勿移开卡片..."
                Operation.DISABLE_SDM -> "正在关闭 SDM，请勿移开卡片..."
                Operation.ENABLE_WRITE_PROTECTION -> "正在增加 File 02 写保护，请勿移开卡片..."
                Operation.DISABLE_WRITE_PROTECTION -> "正在移除 File 02 写保护，请勿移开卡片..."
                Operation.WRITE_FILE_03 -> "正在使用 Demo Key 3 写入 File 03，请勿移开卡片..."
                Operation.READ_FILE_03 -> "正在使用 Demo Key 2 读取 File 03，请勿移开卡片..."
                Operation.INSPECT -> "正在读取卡片，请保持贴卡..."
            }
            binding.ntag424Result.text = "RF UID: ${tag.id.toHex(":")}"
        }
        val inspection = runCatching {
            when (operation) {
                Operation.WRITE_NDEF -> {
                    val request = checkNotNull(writeRequest) { "写入参数已失效，请重新操作" }
                    Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                        val result = client.writeBusinessNdefWithZeroKey0(request.url, request.applicationPackage)
                        "NDEF 写入成功\n" +
                            "records: ${result.recordCount} (URL + pkg)\n" +
                            "File 02 bytes: ${result.ndefBytes}\n" +
                            "URL: ${request.url}\n" +
                            "pkg: ${request.applicationPackage}"
                    }
                }
                Operation.ENABLE_SDM -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val result = client.enableSdmWithZeroKey0()
                    "SDM 配置成功\n" +
                        "NDEF bytes: ${result.ndefBytes}\n" +
                        "PICCDataOffset: ${result.piccDataOffset}\n" +
                        "SDMMACOffset: ${result.macOffset}\n\n" +
                        "请移开卡片后重新贴卡，执行只读检测确认 SDM 状态。"
                }
                Operation.DISABLE_SDM -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    client.disableSdmWithDemoKey()
                    "SDM 已关闭\nNDEF 内容、文件通信模式和访问权限保持不变。\n\n" +
                        "现在可以移开卡片，然后使用“写入 URL + pkg 到 NDEF (File 02)”。"
                }
                Operation.ENABLE_WRITE_PROTECTION,
                Operation.DISABLE_WRITE_PROTECTION -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val protected = operation == Operation.ENABLE_WRITE_PROTECTION
                    val result = client.setWriteProtectionWithDemoKey(protected)
                    if (result.protected) {
                        "写保护已增加\nW=Demo Key ${result.writeKey}\nRW=Demo Key ${result.readWriteKey}\n" +
                            "SDM 配置及其他权限保持不变。"
                    } else {
                        "写保护已移除\nW=Free\nRW=Free\nSDM 配置及其他权限保持不变。"
                    }
                }
                Operation.WRITE_FILE_03 -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val text = checkNotNull(file03Text) { "File 03 写入参数已失效，请重新操作" }
                    val file03Result = client.writeFile03DemoData(text)
                    "File 03 写入成功\npayload: ${file03Result.payloadBytes} bytes\n" +
                        "format: 2-byte length + UTF-8 + zero padding\ntext: ${file03Result.text}"
                }
                Operation.READ_FILE_03 -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val file03Result = client.readFile03DemoData()
                    "File 03 读取成功\npayload: ${file03Result.payloadBytes} bytes\n" +
                        "text: ${file03Result.text}\nraw: ${file03Result.rawBytes.toHex(":")}"
                }
                Operation.INSPECT -> Ntag424DnaDemoClient.connect(tag).use { client ->
                    client.inspect().toDisplayText()
                }
            }
        }
        val result = inspection.getOrElse { error ->
            Log.e(
                LOG_TAG,
                "$LOG_PREFIX inspection failed, elapsedMs=${SystemClock.elapsedRealtime() - startedAt}",
                error
            )
            "检测失败: ${error.message ?: error.javaClass.simpleName}\n" +
                "请确认卡片是支持 ISO-DEP 的 NTAG 424 DNA，并保持贴卡到读取完成。"
        }
        if (inspection.isSuccess) {
            Log.i(
                LOG_TAG,
                "$LOG_PREFIX inspection completed, elapsedMs=${SystemClock.elapsedRealtime() - startedAt}"
            )
        }
        runOnUiThread {
            binding.ntag424Status.text = "检测完成；移开卡片后可再次检测"
            binding.ntag424Result.text = result
            processingTag.set(false)
        }
    }

    private fun ByteArray.toHex(separator: String): String =
        joinToString(separator) { "%02X".format(it) }

    private fun confirmBusinessNdefWrite() {
        val request = WriteRequest(
            binding.ntag424Url.text.toString().trim(),
            binding.ntag424Package.text.toString().trim()
        )
        if (request.url.isBlank() || request.applicationPackage.isBlank()) {
            binding.ntag424Status.text = "URL 和 pkg 不能为空"
            return
        }
        AlertDialog.Builder(this)
            .setTitle("确认写入测试卡 NDEF")
            .setMessage(
                "下一次贴指定测试卡将覆盖 File 02，写入两条 records：\n\n" +
                    "1. URL: ${request.url}\n2. pkg: ${request.applicationPackage}\n\n" +
                    "仅支持 SDM 尚未开启、且可由全零 Key 0 写入的测试卡。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = request
                pendingOperation.set(Operation.WRITE_NDEF)
                binding.ntag424Status.text = "已授权一次 NDEF 写入，请贴指定测试卡"
                binding.ntag424Result.text = "等待 UID 04:18:72:62:E5:16:90"
                Log.w(LOG_TAG, "$LOG_PREFIX business NDEF write armed for one tag")
            }
            .show()
    }

    private fun confirmZeroKeySdmConfiguration() {
        AlertDialog.Builder(this)
            .setTitle("确认开启测试卡 SDM")
            .setMessage(
                "仅允许 UID 04:18:72:62:E5:16:90。下一次贴卡将使用 16 字节全零 Key 0 " +
                    "执行 EV2 认证，在保留当前业务 URI 参数和其他 NDEF records 的前提下追加 SDM 字段，" +
                    "并开启 SDM。该配置使用全零测试密钥，绝不能用于生产。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingOperation.set(Operation.ENABLE_SDM)
                binding.ntag424Status.text = "已授权一次 SDM 配置，请贴指定测试卡"
                binding.ntag424Result.text =
                    "等待 UID 04:18:72:62:E5:16:90\n认证失败或 UID 不匹配时不会发送写入命令。"
                Log.w(LOG_TAG, "$LOG_PREFIX zero-Key SDM configuration armed for one tag")
            }
            .show()
    }

    private fun confirmWriteProtection(protected: Boolean) {
        val operation = if (protected) {
            Operation.ENABLE_WRITE_PROTECTION
        } else {
            Operation.DISABLE_WRITE_PROTECTION
        }
        val action = if (protected) "增加" else "移除"
        AlertDialog.Builder(this)
            .setTitle("确认${action}测试卡写保护")
            .setMessage(
                "下一次贴指定测试卡只会${action} File 02 的写保护。" +
                    if (protected) {
                        "W 和 RW 将使用统一的 Demo Key 0；SDM、读取权限、Change 权限和文件内容保持不变。"
                    } else {
                        "W 和 RW 将恢复为 Free；SDM、读取权限、Change 权限和文件内容保持不变。"
                    }
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingOperation.set(operation)
                binding.ntag424Status.text = "已授权一次${action}写保护，请贴指定测试卡"
                binding.ntag424Result.text = "等待 UID 04:18:72:62:E5:16:90"
                Log.w(LOG_TAG, "$LOG_PREFIX $operation armed for one tag")
            }
            .show()
    }

    private fun confirmDisableSdm() {
        AlertDialog.Builder(this)
            .setTitle("确认关闭测试卡 SDM")
            .setMessage(
                "下一次贴指定测试卡只会关闭 File 02 的 SDM。NDEF 内容、文件通信模式和全部访问权限保持不变。" +
                    "关闭后可再使用写入 NDEF 功能覆盖 URL + pkg。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingOperation.set(Operation.DISABLE_SDM)
                binding.ntag424Status.text = "已授权一次关闭 SDM，请贴指定测试卡"
                binding.ntag424Result.text = "等待 UID 04:18:72:62:E5:16:90"
                Log.w(LOG_TAG, "$LOG_PREFIX disable SDM armed for one tag")
            }
            .show()
    }

    private fun confirmFile03Write() {
        val text = binding.ntag424File03Data.text.toString()
        val byteCount = text.toByteArray(Charsets.UTF_8).size
        if (byteCount > 126) {
            binding.ntag424Status.text = "File 03 测试文本最多 126 bytes UTF-8，当前 $byteCount bytes"
            return
        }
        AlertDialog.Builder(this)
            .setTitle("确认写入 File 03")
            .setMessage(
                "下一次贴指定测试卡将使用 Demo Key 3 覆盖 128-byte File 03。\n\n" +
                    "测试格式：2-byte 长度 + UTF-8 文本 + 零填充\n" +
                    "payload: $byteCount bytes\ntext: $text\n\nFile 02、NDEF 和 SDM 不会改变。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingFile03Text = text
                pendingOperation.set(Operation.WRITE_FILE_03)
                binding.ntag424Status.text = "已授权一次 File 03 写入，请贴指定测试卡"
            }
            .show()
    }

    private fun armFile03Read() {
        pendingWriteRequest = null
        pendingFile03Text = null
        pendingOperation.set(Operation.READ_FILE_03)
        binding.ntag424Status.text = "已准备使用 Demo Key 2 读取 File 03，请贴指定测试卡"
        binding.ntag424Result.text = "等待 UID 04:18:72:62:E5:16:90"
    }

    companion object {
        private const val LOG_TAG = "Ntag424DnaTest"
        private const val LOG_PREFIX = "[NTAG424-DEMO]"
    }

    private enum class Operation {
        INSPECT,
        WRITE_NDEF,
        ENABLE_SDM,
        DISABLE_SDM,
        ENABLE_WRITE_PROTECTION,
        DISABLE_WRITE_PROTECTION,
        WRITE_FILE_03,
        READ_FILE_03
    }
    private data class WriteRequest(val url: String, val applicationPackage: String)
}
