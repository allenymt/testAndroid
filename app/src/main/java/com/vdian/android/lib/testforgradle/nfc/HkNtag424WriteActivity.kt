package com.vdian.android.lib.testforgradle.nfc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.databinding.ActivityHkNtag424WriteBinding
import java.util.concurrent.atomic.AtomicBoolean

class HkNtag424WriteActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityHkNtag424WriteBinding
    private lateinit var store: HkNtag424ProductionStore
    private lateinit var records: List<HkNtag424Record>
    private var nfcAdapter: NfcAdapter? = null
    private val processing = AtomicBoolean(false)
    private var selectedRecordIndex: Int? = null
    @Volatile private var armedAssignment: HkNtag424Assignment? = null
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
        store = HkNtag424ProductionStore(this)
        records = runCatching { HkNtag424RecordRepository(this).load() }.getOrElse { error ->
            binding.status.text = "Mock 数据加载失败"
            binding.result.text = error.message ?: error.javaClass.simpleName
            emptyList()
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        binding.writeCard.setOnClickListener { armWrite() }
        binding.disableSdm.setOnClickListener { armDisableSdm() }
        binding.clearNdef.setOnClickListener { armClearNdef() }
        binding.resetKeys.setOnClickListener { armResetKeys() }
        binding.clearSelection.setOnClickListener { clearSelection() }
        binding.openNfcSettings.setOnClickListener { BuyerNFCUtil.Util.toOpenNfc(this) }
        binding.searchHistory.setOnClickListener {
            renderHistory(binding.key1Query.text.toString(), announce = true)
        }
        binding.showAllHistory.setOnClickListener {
            binding.key1Query.setText("")
            renderHistory(announce = true)
        }
        binding.clearProductionData.setOnClickListener { confirmClearProductionData() }
        renderRecordOptions()
        renderAssignment()
        renderHistory()
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

    private fun armWrite() {
        val assignment = selectedAssignment(requireUnused = true) ?: return
        armedAssignment = assignment
        armedOperation = Operation.WRITE
        binding.status.text = "已准备 ${assignment.record.recordId} / ${assignment.physicalSerialNo}，请贴卡并保持不动"
        binding.result.text = "即将执行：写入 Key 0/1/2、完整 NDEF，并开启 SDM"
    }

    private fun armDisableSdm() {
        val assignment = selectedAssignment(requireUnused = false) ?: return
        armedAssignment = assignment
        armedOperation = Operation.DISABLE_SDM
        binding.status.text = "已准备使用 ${assignment.record.recordId} 的 Key 0 关闭 SDM，请贴卡"
        binding.result.text = "只关闭 File 02 SDM 并清理外层 e/c；不会消耗 SN 或删除历史"
    }

    private fun armClearNdef() {
        val assignment = selectedAssignment(requireUnused = false) ?: return
        armedAssignment = assignment
        armedOperation = Operation.CLEAR_NDEF
        binding.status.text = "已准备使用 ${assignment.record.recordId} 的 Key 0 清空 NDEF，请贴卡"
        binding.result.text = "要求卡片已关闭 SDM；只把 File 02 NLEN 清零，不修改密钥、历史或 SN"
    }

    private fun armResetKeys() {
        val assignment = selectedAssignment(requireUnused = false) ?: return
        armedAssignment = assignment
        armedOperation = Operation.RESET_KEYS
        binding.status.text = "已准备使用 ${assignment.record.recordId} 重置 Key 0/1/2，请贴卡"
        binding.result.text =
            "将按 Key 1 → Key 2 → Key 0 恢复为全零 Transport Key/version 0；NDEF、SDM、历史和 SN 不变"
    }

    override fun onTagDiscovered(tag: Tag) {
        val assignment = armedAssignment ?: return
        val operation = armedOperation ?: return
        if (!processing.compareAndSet(false, true)) return
        armedAssignment = null
        armedOperation = null
        runOnUiThread {
            binding.status.text = when (operation) {
                Operation.WRITE -> "正在写卡，请勿移开卡片..."
                Operation.DISABLE_SDM -> "正在关闭 SDM，请勿移开卡片..."
                Operation.CLEAR_NDEF -> "正在清空 NDEF，请勿移开卡片..."
                Operation.RESET_KEYS -> "正在重置 Key 0/1/2，请勿移开卡片..."
            }
        }
        val result = runCatching {
            Ntag424DnaPersonalizationClient.connect(tag).use { client ->
                when (operation) {
                    Operation.WRITE -> client.personalizeProductionRecord(
                        url = assignment.record.url,
                        applicationPackage = assignment.record.packageName,
                        key0 = assignment.record.key0Bytes,
                        key1 = assignment.record.key1Bytes,
                        key2 = assignment.record.key2Bytes,
                        keyVersion = assignment.record.version
                    )
                    Operation.DISABLE_SDM -> client.disableSdmWithKey0(assignment.record.key0Bytes)
                    Operation.CLEAR_NDEF -> client.clearNdefWithKey0(assignment.record.key0Bytes)
                    Operation.RESET_KEYS -> client.resetProductionKeysToTransport(
                        key0 = assignment.record.key0Bytes,
                        key1 = assignment.record.key1Bytes,
                        key2 = assignment.record.key2Bytes,
                        productionVersion = assignment.record.version
                    )
                }
            }
        }
        runOnUiThread {
            processing.set(false)
            result.onSuccess { operationResult ->
                when (operationResult) {
                    is Ntag424DnaPersonalizationClient.ProductionSetupResult -> runCatching {
                        store.complete(assignment, operationResult.uid.toHex())
                    }.onSuccess { completed ->
                        binding.status.text = "写卡成功，已生成发行 JSON"
                        binding.result.text = store.toJson(completed)
                        renderHistory()
                        clearSelection(showStatus = false)
                        renderRecordOptions()
                    }.onFailure { error ->
                        Log.e(LOG_TAG, "card completed but production progress save failed", error)
                        binding.status.text = "卡片写入成功，但生产进度保存失败，请停止继续生产"
                        binding.result.text = error.message ?: error.javaClass.simpleName
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
                        binding.result.text = "File 02 NLEN 已写为 0；密钥、FileSettings、历史和 SN 均未修改"
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
                renderAssignment()
            }.onFailure { error ->
                Log.e(LOG_TAG, "write failed, recordId=${assignment.record.recordId}", error)
                binding.status.text = "写卡失败；当前 SN 已保留，可直接重试"
                binding.result.text = error.message ?: error.javaClass.simpleName
                renderAssignment()
            }
        }
    }

    private fun renderAssignment() {
        val assignment = selectedRecordIndex?.let { index ->
            runCatching { store.assignmentFor(index, records, requireUnused = false) }.getOrNull()
        }
        binding.currentRecord.text = if (assignment == null) {
            "尚未选择生产数据"
        } else {
            "批次 ${assignment.record.batchId}  |  ${assignment.record.recordId}\n" +
                "SN ${assignment.physicalSerialNo}  |  KeyVersion ${assignment.record.version}\n" +
                "Key 0 固定 / Key 1 每卡独立 / Key 2 固定\n\n" +
                assignment.record.url
        }
        val enabled = assignment != null && !processing.get()
        binding.writeCard.isEnabled = enabled
        binding.disableSdm.isEnabled = enabled
        binding.clearNdef.isEnabled = enabled
        binding.resetKeys.isEnabled = enabled
        binding.clearSelection.isEnabled = selectedRecordIndex != null && !processing.get()
    }

    private fun renderHistory(key1: String? = null, announce: Boolean = false) {
        val history = store.history(key1)
        binding.historyList.removeAllViews()
        binding.historyEmpty.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
        binding.historyEmpty.text = if (key1.isNullOrBlank()) "暂无历史写入记录" else "未找到该 Key 1 对应的记录"
        history.forEach { record -> binding.historyList.addView(createHistoryRow(record)) }
        if (announce) {
            binding.status.text = if (key1.isNullOrBlank()) {
                "已显示全部历史记录，共 ${history.size} 条"
            } else {
                "Key 1 查询完成，共 ${history.size} 条"
            }
            Toast.makeText(this, "已加载 ${history.size} 条记录", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderRecordOptions() {
        binding.recordOptions.removeAllViews()
        records.forEachIndexed { index, record ->
            binding.recordOptions.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = "${record.recordId}  |  Key 1 ${record.key1.take(8)}..." +
                    if (store.isIssued(record.recordId)) "  [已写入]" else ""
                isChecked = selectedRecordIndex == index
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedRecordIndex = index
                        renderAssignment()
                        binding.status.text = "已选择 ${record.recordId}"
                    }
                }
            })
        }
    }

    private fun selectedAssignment(requireUnused: Boolean): HkNtag424Assignment? {
        val index = selectedRecordIndex
        if (index == null) {
            binding.status.text = "请先选择一条生产数据"
            return null
        }
        return runCatching { store.assignmentFor(index, records, requireUnused) }.getOrElse { error ->
            binding.status.text = error.message ?: "所选生产数据不可用"
            null
        }
    }

    private fun clearSelection(showStatus: Boolean = true) {
        selectedRecordIndex = null
        armedAssignment = null
        armedOperation = null
        binding.recordOptions.clearCheck()
        renderAssignment()
        if (showStatus) binding.status.text = "已清除选择"
    }

    private fun confirmClearProductionData() {
        if (processing.get()) {
            binding.status.text = "当前正在操作卡片，暂不能清除生产数据"
            return
        }
        AlertDialog.Builder(this)
            .setTitle("清除本机生产数据？")
            .setMessage("将删除全部历史记录、已发行 UID、已使用 recordId 和 SN 进度，并从 No.001 重新开始。不会修改卡片或 assets 配置。此操作不可撤销。")
            .setNegativeButton("取消", null)
            .setPositiveButton("确认清除") { _, _ ->
                runCatching { store.clearProductionData() }
                    .onSuccess {
                        clearSelection(showStatus = false)
                        binding.key1Query.setText("")
                        renderRecordOptions()
                        renderHistory()
                        binding.status.text = "本机生产数据已清除，SN 将从 No.001 重新开始"
                        binding.result.text = "历史记录、已发行 UID 和已使用 recordId 均已清除；卡片和 assets 配置未修改"
                    }
                    .onFailure { error ->
                        binding.status.text = "清除本机生产数据失败"
                        binding.result.text = error.message ?: error.javaClass.simpleName
                    }
            }
            .show()
    }

    private fun createHistoryRow(record: HkNtag424Record): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
            setBackgroundColor(0xFFF4F4F1.toInt())
        }
        container.addView(TextView(this).apply {
            text = "${record.physicalSerialNo}  ${record.recordId}\nUID: ${record.uid}\nKey 1: ${record.key1}"
            setTextColor(0xFF111111.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "复制 JSON"
            setOnClickListener { copyJson(record) }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(Button(this).apply {
            text = "删除"
            setOnClickListener {
                store.deleteHistory(record.recordId)
                renderHistory(binding.key1Query.text.toString(), announce = true)
            }
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        container.addView(actions)
        return container.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8.dp }
        }
    }

    private fun copyJson(record: HkNtag424Record) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NTAG 424 production record", store.toJson(record)))
        binding.status.text = "${record.recordId} JSON 已复制"
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    companion object {
        private const val LOG_TAG = "HkNtag424Write"
    }

    private enum class Operation { WRITE, DISABLE_SDM, CLEAR_NDEF, RESET_KEYS }
}
