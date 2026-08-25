package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
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
    private val mockServerVerifier = Ntag424MockServerVerifier()
    @Volatile private var pendingWriteRequest: WriteRequest? = null
    @Volatile private var pendingFile03Text: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOG_TAG, "$LOG_PREFIX page created, restored=${savedInstanceState != null}")
        binding = ActivityNtag424DnaTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val main = findViewById<View>(R.id.main)
        val initialPadding = intArrayOf(main.paddingLeft, main.paddingTop, main.paddingRight, main.paddingBottom)
        ViewCompat.setOnApplyWindowInsetsListener(main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialPadding[0] + systemBars.left,
                initialPadding[1] + systemBars.top,
                initialPadding[2] + systemBars.right,
                initialPadding[3] + systemBars.bottom
            )
            insets
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        binding.openNfcSettings.setOnClickListener { BuyerNFCUtil.Util.toOpenNfc(this) }
        binding.readNtag424Card.setOnClickListener {
            pendingWriteRequest = null
            pendingFile03Text = null
            pendingOperation.set(Operation.INSPECT)
            Log.i(LOG_TAG, "$LOG_PREFIX card content read armed by user")
            binding.ntag424Status.text = "已准备读取卡内容，请贴 NTAG 424 DNA 卡片"
            binding.ntag424Result.text = getString(R.string.ntag424_waiting_for_tag)
        }
        binding.writeNtag424Ndef.setOnClickListener { confirmBusinessNdefWrite() }
        binding.enableSdmZeroKey.setOnClickListener { confirmDemoKeySdmConfiguration() }
        binding.enableRandomUid.setOnClickListener { confirmEnableRandomUid() }
        binding.disableSdmZeroKey.setOnClickListener { confirmDisableSdm() }
        binding.enableNtag424WriteProtection.setOnClickListener {
            confirmWriteProtection(protected = true)
        }
        binding.disableNtag424WriteProtection.setOnClickListener {
            confirmWriteProtection(protected = false)
        }
        binding.writeNtag424File03.setOnClickListener { confirmFile03Write() }
        binding.readNtag424File03.setOnClickListener { armFile03Read() }
        binding.changeNtag424Key0.setOnClickListener { confirmChangeDemoKey(0) }
        binding.resetNtag424Keys.setOnClickListener { confirmResetDemoKeys() }
        binding.changeNtag424Key1.setOnClickListener { confirmChangeDemoKey(1) }
        binding.changeNtag424Key2.setOnClickListener { confirmChangeDemoKey(2) }
        binding.personalizeKeysAndVerifySdm.setOnClickListener { confirmCompositeSetup() }
        binding.verifyNtag424SdmUrl.setOnClickListener { armSdmUrlVerification() }
        binding.showNtag424File02.setOnClickListener { scrollToSection(binding.ntag424File02Section) }
        binding.showNtag424Keys.setOnClickListener { scrollToSection(binding.ntag424KeysSection) }
        binding.showNtag424File03.setOnClickListener { scrollToSection(binding.ntag424File03Section) }
        binding.showNtag424Result.setOnClickListener { scrollToSection(binding.ntag424ResultSection) }
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
                Operation.ENABLE_SDM -> "正在使用当前 Demo Key 0 配置 SDM，请勿移开卡片..."
                Operation.ENABLE_RANDOM_UID -> "正在永久开启 Random UID，请勿移开卡片..."
                Operation.DISABLE_SDM -> "正在关闭 SDM，请勿移开卡片..."
                Operation.ENABLE_WRITE_PROTECTION -> "正在增加 File 02 写保护，请勿移开卡片..."
                Operation.DISABLE_WRITE_PROTECTION -> "正在移除 File 02 写保护，请勿移开卡片..."
                Operation.WRITE_FILE_03 -> "正在使用 Demo Key 3 写入 File 03，请勿移开卡片..."
                Operation.READ_FILE_03 -> "正在使用 Demo Key 2 读取 File 03，请勿移开卡片..."
                Operation.CHANGE_KEY_0 -> "正在修改 AppKey 0，请勿移开卡片..."
                Operation.RESET_KEYS -> "正在把 AppKey 0/1/2 恢复为 transport version 0，请勿移开卡片..."
                Operation.CHANGE_KEY_1 -> "正在修改 AppKey 1，请勿移开卡片..."
                Operation.CHANGE_KEY_2 -> "正在修改 AppKey 2，请勿移开卡片..."
                Operation.PERSONALIZE_KEYS_WRITE_NDEF_ENABLE_SDM ->
                    "正在处理 Key 0/1/2、写入 NDEF 并开启 SDM，请勿移开卡片..."
                Operation.VERIFY_SDM_URL -> "正在读取动态 SDM URL 并验证，请保持贴卡..."
                Operation.INSPECT -> "正在读取卡片，请保持贴卡..."
            }
            binding.ntag424Result.text = "RF UID: ${tag.id.toHex(":")}"
        }
        val inspection = runCatching {
            when (operation) {
                Operation.WRITE_NDEF -> {
                    val request = checkNotNull(writeRequest) { "写入参数已失效，请重新操作" }
                    Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                        val result = client.writeBusinessNdefWithDemoKey0(request.url, request.applicationPackage)
                        "NDEF 写入成功\n" +
                            "records: ${result.recordCount} (URL + pkg)\n" +
                            "File 02 bytes: ${result.ndefBytes}\n" +
                            "URL: ${request.url}\n" +
                            "pkg: ${request.applicationPackage}"
                    }
                }
                Operation.ENABLE_SDM -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val result = client.enableSdmWithDemoKey0()
                    "SDM 配置成功\n" +
                        "NDEF bytes: ${result.ndefBytes}\n" +
                        "PICCDataOffset: ${result.piccDataOffset}\n" +
                        "SDMMACOffset: ${result.macOffset}\n\n" +
                        "请移开卡片后重新贴卡，执行只读检测确认 SDM 状态。"
                }
                Operation.ENABLE_RANDOM_UID -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    client.enableRandomUidWithDemoKey0()
                    "Random UID 已开启\n" +
                        "该配置不可逆，后续每次 RF 识别可能返回不同 UID。\n" +
                        "SDM、NDEF、File 02/03 内容和密钥未修改。"
                }
                Operation.DISABLE_SDM -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val result = client.disableSdmWithDemoKey()
                    "SDM 已关闭\n" +
                        "e/c 参数: ${if (result.urlParametersRemoved) "已从 NDEF URL 移除" else "未发现"}\n" +
                        "业务 query、pkg record、文件通信模式和访问权限保持不变。\n\n" +
                        "现在可以移开卡片后再次开启 SDM，或写入新的 URL + pkg。"
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
                Operation.CHANGE_KEY_0,
                Operation.CHANGE_KEY_1,
                Operation.CHANGE_KEY_2 -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val keyNumber = when (operation) {
                        Operation.CHANGE_KEY_0 -> 0
                        Operation.CHANGE_KEY_1 -> 1
                        else -> 2
                    }
                    val changeResult = client.changeDemoKey(keyNumber)
                    "AppKey ${changeResult.keyNumber} 修改成功\n" +
                        "version: ${changeResult.oldVersion} -> ${changeResult.newVersion}\n" +
                        "密钥值为代码内固定 Demo 值，不会输出到日志或页面。"
                }
                Operation.RESET_KEYS -> Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                    val restore = client.restoreDemoKeysToTransport()
                    val initialVersions = restore.initialKeyVersions.entries.sortedBy { it.key }
                        .joinToString { "Key ${it.key}: v${it.value} -> v0" }
                    val restoredKeys = restore.restoredKeys.joinToString()
                        .ifEmpty { "无（均已是 v0）" }
                    "AppKey 0/1/2 已恢复为全零 Transport Key\n" +
                        "初始/目标版本: $initialVersions\n" +
                        "本次恢复顺序: $restoredKeys\n" +
                        "写后已逐个使用全零密钥重新认证。NDEF、SDM 和 FileSettings 保持不变。"
                }
                Operation.PERSONALIZE_KEYS_WRITE_NDEF_ENABLE_SDM ->
                    Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                        val request = checkNotNull(writeRequest) { "复合操作的 NDEF 参数已失效，请重新操作" }
                        val composite = client.personalizeDemoKeysWriteNdefAndEnableSdm(
                            request.url,
                            request.applicationPackage
                        )
                        val versions = composite.initialKeyVersions.entries
                            .sortedBy { it.key }
                            .joinToString {
                                "Key ${it.key}: v${it.value} -> v${Ntag424DemoKeys.PERSONALIZED_VERSION}"
                            }
                        val changed = composite.changedKeys.joinToString()
                            .ifEmpty {
                                "无（均已是 v${Ntag424DemoKeys.PERSONALIZED_VERSION}，已逐个认证）"
                            }
                        "复合操作成功\n" +
                            "初始/目标版本: $versions\n" +
                            "本次修改顺序: $changed\n" +
                            "NDEF 写入: ${composite.writeResult.recordCount} records, " +
                            "${composite.writeResult.ndefBytes} bytes\n" +
                            "URL: ${request.url}\n" +
                            "pkg: ${request.applicationPackage}\n" +
                            "SDM 已开启\n" +
                            "PICCDataOffset: ${composite.sdmResult.piccDataOffset}\n" +
                            "SDMMACOffset: ${composite.sdmResult.macOffset}\n\n" +
                            "请移开卡片后重新贴卡，再执行“读卡并自动验证 SDM URL”。"
                    }
                Operation.VERIFY_SDM_URL -> Ntag424DnaDemoClient.connect(tag).use { client ->
                    val report = client.inspect()
                    check(report.ndefSettings?.sdmEnabled == true) { "File 02 尚未开启 SDM" }
                    val dynamicUrl = checkNotNull(report.ndefUri) { "File 02 没有可验证的 URI record" }.toString()
                    val verification = mockServerVerifier.verify(dynamicUrl)
                    "服务器验证成功\n" +
                        "UID: ${verification.uid.toHex(":")}\n" +
                        "SDMReadCtr: ${verification.readCounter}\n" +
                        "c: ${verification.calculatedMac.toHex("")}\n" +
                        "计数器已原子记录；旧计数器将按重放拒绝。"
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
            scrollToSection(binding.ntag424ResultSection)
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
            .setTitle("确认写入卡片 NDEF")
            .setMessage(
                "下一次贴 NTAG 424 DNA 卡片将覆盖其 File 02，写入两条 records：\n\n" +
                    "1. URL: ${request.url}\n2. pkg: ${request.applicationPackage}\n\n" +
                    "仅支持 SDM 尚未开启、且可由当前 Demo Key 0 写入的卡片。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = request
                pendingOperation.set(Operation.WRITE_NDEF)
                binding.ntag424Status.text = "已授权一次 NDEF 写入，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text = "等待 NTAG 424 DNA 卡片"
                Log.w(LOG_TAG, "$LOG_PREFIX business NDEF write armed for one tag")
            }
            .show()
    }

    private fun confirmDemoKeySdmConfiguration() {
        AlertDialog.Builder(this)
            .setTitle("确认开启卡片 SDM")
            .setMessage(
                "下一次贴 NTAG 424 DNA 卡片将使用其当前版本对应的 Demo Key 0 " +
                    "执行 EV2 认证，在保留当前业务 URI 参数和其他 NDEF records 的前提下追加 SDM 字段，" +
                    "并开启 SDM。代码内固定 Demo 密钥绝不能用于生产。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingOperation.set(Operation.ENABLE_SDM)
                binding.ntag424Status.text = "已授权一次 SDM 配置，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text =
                    "等待 NTAG 424 DNA 卡片\n卡型识别或认证失败时不会发送写入命令。"
                Log.w(LOG_TAG, "$LOG_PREFIX Demo-Key SDM configuration armed for one tag")
            }
            .show()
    }

    private fun confirmEnableRandomUid() {
        AlertDialog.Builder(this)
            .setTitle("确认永久开启 Random UID")
            .setMessage(
                "下一次贴卡将使用当前 Demo Key 0 执行 SetConfiguration，永久开启 PICC Random UID。\n\n" +
                    "开启后不能关闭或恢复真实 RF UID；SDM/NDEF/密钥内容不变，但 Android 读取到的 RF UID 可能每次不同。\n\n" +
                    "仅建议在测试卡上执行。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingFile03Text = null
                pendingOperation.set(Operation.ENABLE_RANDOM_UID)
                binding.ntag424Status.text = "已授权一次永久开启 Random UID，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text = "等待卡片；此操作不可逆，请保持贴卡直到完成。"
                Log.w(LOG_TAG, "$LOG_PREFIX Random UID enable armed for one tag")
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
            .setTitle("确认${action}卡片写保护")
            .setMessage(
                "下一次贴 NTAG 424 DNA 卡片只会${action}其 File 02 的写保护。" +
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
                binding.ntag424Status.text = "已授权一次${action}写保护，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text = "等待 NTAG 424 DNA 卡片"
                Log.w(LOG_TAG, "$LOG_PREFIX $operation armed for one tag")
            }
            .show()
    }

    private fun confirmDisableSdm() {
        AlertDialog.Builder(this)
            .setTitle("确认关闭卡片 SDM")
            .setMessage(
                "下一次贴 NTAG 424 DNA 卡片会关闭其 File 02 SDM，并从第一条 URL record 中移除顶层 e/c 参数。" +
                    "业务 query、pkg record、文件通信模式和全部访问权限保持不变。" +
                    "关闭后可再次开启 SDM，也可使用写入 NDEF 功能覆盖 URL + pkg。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingOperation.set(Operation.DISABLE_SDM)
                binding.ntag424Status.text = "已授权一次关闭 SDM，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text = "等待 NTAG 424 DNA 卡片"
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
                "下一次贴 NTAG 424 DNA 卡片将使用 Demo Key 3 覆盖其 128-byte File 03。\n\n" +
                    "测试格式：2-byte 长度 + UTF-8 文本 + 零填充\n" +
                    "payload: $byteCount bytes\ntext: $text\n\nFile 02、NDEF 和 SDM 不会改变。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingFile03Text = text
                pendingOperation.set(Operation.WRITE_FILE_03)
                binding.ntag424Status.text = "已授权一次 File 03 写入，请贴 NTAG 424 DNA 卡片"
            }
            .show()
    }

    private fun armFile03Read() {
        pendingWriteRequest = null
        pendingFile03Text = null
        pendingOperation.set(Operation.READ_FILE_03)
        binding.ntag424Status.text = "已准备使用 Demo Key 2 读取 File 03，请贴 NTAG 424 DNA 卡片"
        binding.ntag424Result.text = "等待 NTAG 424 DNA 卡片"
    }

    private fun confirmChangeDemoKey(keyNumber: Int) {
        val operation = when (keyNumber) {
            0 -> Operation.CHANGE_KEY_0
            1 -> Operation.CHANGE_KEY_1
            else -> Operation.CHANGE_KEY_2
        }
        val orderWarning = if (keyNumber == 0) {
            "建议在 Key 1、Key 2 修改并验证完成后最后修改 Key 0。"
        } else {
            "该命令使用当前 Key 0 授权。"
        }
        AlertDialog.Builder(this)
            .setTitle("确认修改 AppKey $keyNumber")
            .setMessage(
                "下一次贴 NTAG 424 DNA 卡片会把其 AppKey $keyNumber 从 transport version 0 或旧版 " +
                    "Demo version ${Ntag424DemoKeys.LEGACY_VERSION} 修改为代码内固定的 Demo version " +
                    "${Ntag424DemoKeys.PERSONALIZED_VERSION}。\n\n" +
                    "$orderWarning\n该操作不会修改 FileSettings 或其他 AppKey。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingFile03Text = null
                pendingOperation.set(operation)
                binding.ntag424Status.text = "已授权一次修改 AppKey $keyNumber，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text = "等待 NTAG 424 DNA 卡片"
            }
            .show()
    }

    private fun confirmResetDemoKeys() {
        AlertDialog.Builder(this)
            .setTitle("确认恢复 AppKey 0/1/2 为 version 0")
            .setMessage(
                "下一次贴卡会按 Key 1 → Key 2 → Key 0 的顺序，把 AppKey 0、1、2 恢复为全零 " +
                    "Transport Key/version 0，并在每次修改后使用全零密钥重新认证。\n\n" +
                    "NDEF、SDM、FileSettings、File 03 和 AppKey 3/4 均保持不变。若 SDM 仍开启，" +
                    "动态 e/c 将改为使用全零 Key 2/1；服务端也必须使用对应密钥验证。\n\n" +
                    "操作顺序支持中途移卡后重试，但三次换钥不构成卡片事务，请保持贴卡直至完成。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = null
                pendingFile03Text = null
                pendingOperation.set(Operation.RESET_KEYS)
                binding.ntag424Status.text = "已授权一次恢复 AppKey 0/1/2，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text = "等待 NTAG 424 DNA 卡片"
            }
            .show()
    }

    private fun confirmCompositeSetup() {
        val request = WriteRequest(
            binding.ntag424Url.text.toString().trim(),
            binding.ntag424Package.text.toString().trim()
        )
        if (request.url.isBlank() || request.applicationPackage.isBlank()) {
            binding.ntag424Status.text = "URL 和 pkg 不能为空"
            return
        }
        AlertDialog.Builder(this)
            .setTitle("确认一次贴卡完成复合配置")
            .setMessage(
                "下一次贴卡会在同一次 NFC 连接中执行：\n\n" +
                    "1. 修改或确认 AppKey 0、1、2 为固定 Demo Key/version " +
                    "${Ntag424DemoKeys.PERSONALIZED_VERSION}\n" +
                    "2. 向 NDEF (File 02) 写入 URL + pkg\n" +
                    "3. 使用新 Key 0 开启 SDM\n\n" +
                    "URL: ${request.url}\n" +
                    "pkg: ${request.applicationPackage}\n\n" +
                    "要求 File 02 初始未开启 SDM，并允许 Demo Key 0 写入和修改设置。" +
                    "底层 Key 修改仍采用 Key 1 → Key 2 → Key 0 的安全顺序。中途移卡后，" +
                    "已完成的 Key 可在重试时跳过，但三个阶段不构成卡片事务。"
            )
            .setNegativeButton("取消", null)
            .setPositiveButton("确认，等待贴卡") { _, _ ->
                pendingWriteRequest = request
                pendingFile03Text = null
                pendingOperation.set(Operation.PERSONALIZE_KEYS_WRITE_NDEF_ENABLE_SDM)
                binding.ntag424Status.text = "已准备 Key + NDEF + SDM 复合配置，请贴 NTAG 424 DNA 卡片"
                binding.ntag424Result.text = "等待卡片；请保持贴卡直到 Key、NDEF 和 SDM 三个阶段全部完成"
            }
            .show()
    }

    private fun armSdmUrlVerification() {
        pendingWriteRequest = null
        pendingFile03Text = null
        pendingOperation.set(Operation.VERIFY_SDM_URL)
        binding.ntag424Status.text = "已准备自动读取并验证 SDM URL，请贴卡"
        binding.ntag424Result.text =
            "下一次贴卡将读取 File 02 URI，自动提取 e/c，并使用固定 Demo Key 1/2 验证。"
    }

    private fun scrollToSection(section: View) {
        binding.ntag424ContentScroll.post {
            binding.ntag424ContentScroll.smoothScrollTo(0, section.top)
        }
    }

    companion object {
        private const val LOG_TAG = "Ntag424DnaTest"
        private const val LOG_PREFIX = "[NTAG424-DEMO]"
    }

    private enum class Operation {
        INSPECT,
        WRITE_NDEF,
        ENABLE_SDM,
        ENABLE_RANDOM_UID,
        DISABLE_SDM,
        ENABLE_WRITE_PROTECTION,
        DISABLE_WRITE_PROTECTION,
        WRITE_FILE_03,
        READ_FILE_03,
        CHANGE_KEY_0,
        RESET_KEYS,
        CHANGE_KEY_1,
        CHANGE_KEY_2,
        PERSONALIZE_KEYS_WRITE_NDEF_ENABLE_SDM,
        VERIFY_SDM_URL
    }
    private data class WriteRequest(val url: String, val applicationPackage: String)
}
