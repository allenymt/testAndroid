package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.databinding.ActivityHkNtag424WriteBinding
import java.util.concurrent.atomic.AtomicBoolean

class HkNtag424WriteActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityHkNtag424WriteBinding
    private lateinit var records: List<HkNtag424Record>
    private lateinit var configPanel: HkNfcConfigPanel
    private var nfcAdapter: NfcAdapter? = null
    private val processing = AtomicBoolean(false)
    private var selectedRecordIndex: Int? = null
    @Volatile private var armedRecord: HkNtag424Record? = null
    @Volatile private var armedOperation: Operation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHkNtag424WriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + 16.dp, bars.top + 12.dp, bars.right + 16.dp, bars.bottom + 12.dp)
            insets
        }
        records = runCatching { HkNtag424RecordRepository(this).load() }.getOrElse { error ->
            binding.status.text = "Mock 数据加载失败"
            binding.result.text = error.message ?: error.javaClass.simpleName
            emptyList()
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        configPanel = HkNfcConfigPanel(this, binding.configTools, HkNtag424RecordRepository.ASSET_NAME) {
            selectedRecordIndex
        }
        binding.writeCard.setOnClickListener { arm(Operation.WRITE) }
        binding.disableSdm.setOnClickListener { arm(Operation.DISABLE_SDM) }
        binding.clearNdef.setOnClickListener { arm(Operation.CLEAR_NDEF) }
        binding.resetKeys.setOnClickListener { arm(Operation.RESET_KEYS) }
        binding.clearSelection.setOnClickListener { clearSelection() }
        binding.openNfcSettings.setOnClickListener { BuyerNFCUtil.Util.toOpenNfc(this) }
        renderRecordOptions()
        renderSelection()
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter
        when {
            adapter == null -> binding.status.text = "当前设备不支持 NFC"
            !adapter.isEnabled -> binding.status.text = "NFC 未开启，请先打开系统 NFC 设置"
            else -> adapter.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
    }

    override fun onPause() {
        nfcAdapter?.disableReaderMode(this)
        super.onPause()
    }

    private fun arm(operation: Operation) {
        val record = selectedRecord() ?: return
        armedRecord = record
        armedOperation = operation
        binding.status.text = when (operation) {
            Operation.WRITE -> "已准备写入所选数据，请贴卡并保持不动"
            Operation.DISABLE_SDM -> "已准备使用所选 Key 0 关闭 SDM，请贴卡"
            Operation.CLEAR_NDEF -> "已准备使用所选 Key 0 清空 NDEF，请贴卡"
            Operation.RESET_KEYS -> "已准备使用所选 Key 重置 Key 0/1/2，请贴卡"
        }
        binding.result.text = when (operation) {
            Operation.WRITE -> "将写入 Key 0/1/2、完整 NDEF，并开启 SDM；成功后将 UID 回写所选 JSON 项的 rfid"
            Operation.DISABLE_SDM -> "只关闭 File 02 SDM 并清理外层 e/c"
            Operation.CLEAR_NDEF -> "要求卡片已关闭 SDM；只把 File 02 NLEN 清零"
            Operation.RESET_KEYS -> "按 Key 1 → Key 2 → Key 0 恢复为全零 Transport Key/version 0"
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        val record = armedRecord ?: return
        val operation = armedOperation ?: return
        if (!processing.compareAndSet(false, true)) return
        armedRecord = null
        armedOperation = null
        runOnUiThread {
            renderSelection()
            binding.status.text = when (operation) {
                Operation.WRITE -> "正在写卡，请勿移开卡片..."
                Operation.DISABLE_SDM -> "正在关闭 SDM，请勿移开卡片..."
                Operation.CLEAR_NDEF -> "正在清空 NDEF，请勿移开卡片..."
                Operation.RESET_KEYS -> "正在重置 Key 0/1/2，请勿移开卡片..."
            }
        }
        var cardWritten = false
        val result = runCatching {
            Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                when (operation) {
                    Operation.WRITE -> client.personalizeProductionRecord(
                        url = record.url,
                        applicationPackage = record.packageName,
                        key0 = record.key0Bytes,
                        key1 = record.key1Bytes,
                        key2 = record.key2Bytes,
                        keyVersion = record.version
                    )
                    Operation.DISABLE_SDM -> client.disableSdmWithKey0(record.key0Bytes)
                    Operation.CLEAR_NDEF -> client.clearNdefWithKey0(record.key0Bytes)
                    Operation.RESET_KEYS -> client.resetProductionKeysToTransport(
                        key0 = record.key0Bytes,
                        key1 = record.key1Bytes,
                        key2 = record.key2Bytes,
                        productionVersion = record.version
                    )
                }
            }.also { written ->
                if (written is Ntag424DnaPersonalizationClient.ProductionSetupResult) {
                    cardWritten = true
                    HkNfcConfigFile(this, HkNtag424RecordRepository.ASSET_NAME).writeRfid(
                        records.indexOfFirst { it === record }, written.uid.toHex()
                    )
                }
            }
        }
        runOnUiThread {
            processing.set(false)
            result.onSuccess { operationResult ->
                when (operationResult) {
                    is Ntag424DnaPersonalizationClient.ProductionSetupResult -> {
                        binding.status.text = "写卡成功，rfid 已回写 JSON"
                        binding.result.text = buildString {
                            appendLine("UID: ${operationResult.uid.toHex()}")
                            appendLine("KeyVersion: ${record.version}")
                            appendLine("NDEF bytes: ${operationResult.writeResult.ndefBytes}")
                            appendLine("PICCData offset: ${operationResult.sdmResult.piccDataOffset}")
                            append("MAC offset: ${operationResult.sdmResult.macOffset}")
                        }
                    }
                    is Ntag424DnaPersonalizationClient.DisableSdmResult -> {
                        binding.status.text = "SDM 已关闭"
                        binding.result.text = if (operationResult.urlParametersRemoved) {
                            "File 02 SDM 已关闭，外层 e/c 已清除"
                        } else {
                            "File 02 SDM 已关闭；NDEF 中未发现可清除的外层 e/c"
                        }
                    }
                    is Ntag424DnaPersonalizationClient.ClearNdefResult -> {
                        binding.status.text = "NDEF 已清空"
                        binding.result.text = "File 02 NLEN 已写为 0；密钥和 FileSettings 未修改"
                    }
                    is Ntag424DnaPersonalizationClient.ProductionResetKeysResult -> {
                        binding.status.text = "Key 0/1/2 已恢复初始状态"
                        binding.result.text = buildString {
                            appendLine("目标状态: 全零 Transport Key / version 0")
                            appendLine(
                                "初始版本: " + operationResult.initialKeyVersions.entries
                                    .sortedBy { it.key }
                                    .joinToString { "Key ${it.key}=v${it.value}" }
                            )
                            append("本次重置顺序: ${operationResult.restoredKeys.joinToString().ifEmpty { "无，均已是 v0" }}")
                        }
                    }
                }
                renderSelection()
            }.onFailure { error ->
                Log.e(LOG_TAG, "card operation failed", error)
                binding.status.text = if (cardWritten) "卡片写入成功，但 rfid 回写 JSON 失败" else "操作失败，可直接重试"
                binding.result.text = error.message ?: error.javaClass.simpleName
                renderSelection()
            }
        }
    }

    private fun renderRecordOptions() {
        binding.recordOptions.removeAllViews()
        records.forEachIndexed { index, record ->
            binding.recordOptions.addView(RadioButton(this).apply {
                id = android.view.View.generateViewId()
                text = "配置 ${index + 1}  |  Key 1 ${record.key1.take(8)}..."
                isChecked = selectedRecordIndex == index
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedRecordIndex = index
                        renderSelection()
                        binding.status.text = "已选择配置 ${index + 1}"
                    }
                }
            })
        }
    }

    private fun renderSelection() {
        configPanel.refresh()
        val record = selectedRecordIndex?.let(records::getOrNull)
        binding.currentRecord.text = if (record == null) {
            "尚未选择写卡数据"
        } else {
            "配置 ${selectedRecordIndex!! + 1}  |  KeyVersion ${record.version}\n" +
                "Key 1 ${record.key1.take(8)}...\n\n${record.url}"
        }
        val enabled = record != null && !processing.get()
        binding.writeCard.isEnabled = enabled
        binding.disableSdm.isEnabled = enabled
        binding.clearNdef.isEnabled = enabled
        binding.resetKeys.isEnabled = enabled
        binding.clearSelection.isEnabled = record != null && !processing.get()
    }

    private fun selectedRecord(): HkNtag424Record? {
        val record = selectedRecordIndex?.let(records::getOrNull)
        if (record == null) binding.status.text = "请先选择一条写卡数据"
        return record
    }

    private fun clearSelection() {
        selectedRecordIndex = null
        armedRecord = null
        armedOperation = null
        binding.recordOptions.clearCheck()
        renderSelection()
        binding.status.text = "已清除选择"
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        configPanel.onActivityResult(requestCode, resultCode, data)
    }
    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    companion object {
        private const val LOG_TAG = "HkNtag424Write"
    }

    private enum class Operation { WRITE, DISABLE_SDM, CLEAR_NDEF, RESET_KEYS }
}
