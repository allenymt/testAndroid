package com.vdian.android.lib.testforgradle.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.databinding.ActivityHkNtag215WriteBinding
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class HkNtag215WriteActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityHkNtag215WriteBinding
    private var adapter: NfcAdapter? = null
    private var selected: HkNtag215Record? = null
    private var records: List<HkNtag215Record> = emptyList()
    private lateinit var configPanel: HkNfcConfigPanel
    private val pending = AtomicReference<HkNtag215Record?>(null)
    private val processing = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHkNtag215WriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + 16.dp, bars.top + 12.dp, bars.right + 16.dp, bars.bottom + 12.dp)
            insets
        }
        adapter = NfcAdapter.getDefaultAdapter(this)
        configPanel = HkNfcConfigPanel(this, binding.configTools, HkNtag215Data.ASSET_NAME) {
            selected?.let { record -> records.indexOfFirst { it === record }.takeIf { it >= 0 } }
        }
        configPanel.refresh()
        binding.writeCard.isEnabled = false
        runCatching {
            HkNtag215Data.parse(HkNfcConfigFile(this, HkNtag215Data.ASSET_NAME).load())
        }.onSuccess { records ->
            this.records = records
            records.forEachIndexed { index, record ->
                binding.recordOptions.addView(RadioButton(this).apply {
                    id = View.generateViewId()
                    text = "配置 ${index + 1}"
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) {
                            selected = record
                            pending.set(null)
                            binding.currentRecord.text = "${record.url}\n\n包名: ${record.packageName}"
                            configPanel.refresh()
                            binding.writeCard.isEnabled = !processing.get()
                            binding.status.text = "已选择配置 ${index + 1}"
                        }
                    }
                })
            }
        }.onFailure {
            binding.status.text = "NTAG215 配置加载失败"
            binding.result.text = it.message ?: it.javaClass.simpleName
        }
        binding.writeCard.setOnClickListener {
            val record = selected ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("写入 NTAG215？")
                .setMessage("将覆盖卡片现有 NDEF，写入所选 URL 和包名。不修改密码或锁定状态。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认，等待贴卡") { _, _ ->
                    pending.set(record)
                    binding.status.text = "已准备写入，请贴 NTAG215 并保持不动"
                }.show()
        }
        binding.openNfcSettings.setOnClickListener { BuyerNFCUtil.Util.toOpenNfc(this) }
    }

    override fun onResume() {
        super.onResume()
        val nfc = adapter
        when {
            nfc == null -> binding.status.text = "当前设备不支持 NFC"
            !nfc.isEnabled -> binding.status.text = "请先开启 NFC"
            // Keep the system NDEF check so Tag exposes the Ndef technology.
            else -> nfc.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, null)
        }
    }

    override fun onPause() {
        pending.set(null)
        adapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        if (!processing.compareAndSet(false, true)) return
        val record = pending.getAndSet(null)
        if (record == null) {
            processing.set(false)
            return
        }
        runOnUiThread {
            binding.writeCard.isEnabled = false
            binding.recordOptions.isEnabled = false
            for (index in 0 until binding.recordOptions.childCount) {
                binding.recordOptions.getChildAt(index).isEnabled = false
            }
            binding.status.text = "正在写入，请勿移开卡片"
        }
        var cardWritten = false
        val result = runCatching {
            val bytes = HkNtag215Writer.write(tag, record)
            cardWritten = true
            HkNfcConfigFile(this, HkNtag215Data.ASSET_NAME).writeRfid(
                records.indexOfFirst { it === record },
                tag.id.joinToString("") { "%02X".format(it) }
            )
            bytes
        }
        runOnUiThread {
            result.onSuccess { bytes ->
                binding.status.text = "NTAG215 写入成功，回读通过，rfid 已回写 JSON"
                binding.result.text = "NDEF: $bytes bytes / 2 条记录\nURL: ${record.url}\n包名: ${record.packageName}"
            }.onFailure { error ->
                Log.e("HkNtag215Write", "NDEF write failed", error)
                binding.status.text = if (cardWritten) "卡片写入成功，但 rfid 回写 JSON 失败" else "写入失败，请重新选择写入后贴卡"
                binding.result.text = error.message ?: error.javaClass.simpleName
            }
            processing.set(false)
            configPanel.refresh()
            binding.writeCard.isEnabled = selected != null
            binding.recordOptions.isEnabled = true
            for (index in 0 until binding.recordOptions.childCount) {
                binding.recordOptions.getChildAt(index).isEnabled = true
            }
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        configPanel.onActivityResult(requestCode, resultCode, data)
    }
}
