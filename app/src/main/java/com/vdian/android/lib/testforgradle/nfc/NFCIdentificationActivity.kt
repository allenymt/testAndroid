package com.vdian.android.lib.testforgradle.nfc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivityNfctestBinding
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


class NFCIdentificationActivity : AppCompatActivity() {
    private lateinit var nfcTestBinding: ActivityNfctestBinding

    private var mNfcAdapter: NfcAdapter? = null
    private lateinit var mPendingIntent: PendingIntent
    private val cardExecutor = Executors.newSingleThreadExecutor()
    private val scanSequence = AtomicInteger()
    private var operation = Operation.READ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (redirectLegacyColdLaunch(intent)) return
        nfcTestBinding = ActivityNfctestBinding.inflate(layoutInflater)
        setContentView(nfcTestBinding.root)
        android.util.Log.i(
            LOG_TAG,
            "$LOG_PREFIX onCreate action=${intent.action}, hasTag=${intent.hasExtra(NfcAdapter.EXTRA_TAG)}, " +
                "restored=${savedInstanceState != null}, defaultOperation=$operation"
        )
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        nfcTestBinding.toNfcSetting.setOnClickListener {
            BuyerNFCUtil.Util.toOpenNfc(this)
        }
        nfcTestBinding.fm1208Url.setText(Fm1208NdefClient.DEFAULT_BUSINESS_URL)
        nfcTestBinding.fm1208Package.setText(Fm1208NdefClient.DEFAULT_APPLICATION_PACKAGE)
        nfcTestBinding.readFm1208.setOnClickListener {
            operation = Operation.READ
            logOperationArmed(operation)
            nfcTestBinding.nfcRecord.text = "已选择自动读取（NDEF / FMCOS），请将卡片贴近手机"
        }
        nfcTestBinding.writeNdef.setOnClickListener {
            operation = Operation.WRITE_NDEF
            logOperationArmed(operation)
            nfcTestBinding.nfcRecord.text = "已选择写入 NDEF，请将 NDEF 卡片贴近手机"
        }
        nfcTestBinding.writeFmcos.setOnClickListener {
            operation = Operation.WRITE_FMCOS
            logOperationArmed(operation)
            nfcTestBinding.nfcRecord.text = "已选择更新 FMCOS，请将已初始化的测试卡贴近手机"
        }
        nfcTestBinding.initializeFmcosTest.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("确认格式化 FMCOS 测试卡")
                .setMessage(
                    "仅允许 UID 77:E9:CC:7D。继续后，下一次碰卡将使用实验性全 FF 测试密钥" +
                        "认证、擦除 MF 下文件，并创建 ADF01 / EF0003。失败会立即停止。"
                )
                .setNegativeButton("取消", null)
                .setPositiveButton("确认，等待碰卡") { _, _ ->
                    operation = Operation.INITIALIZE_FMCOS_TEST
                    logOperationArmed(operation)
                    nfcTestBinding.nfcRecord.text =
                        "已授权初始化 UID 77:E9:CC:7D；请将指定测试卡贴近手机"
                }
                .show()
        }
        handleColdLaunch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.i(LOG_TAG, "$LOG_PREFIX onNewIntent action=${intent.action}, flags=${intent.flags}")
        setIntent(intent)
        if (handleColdLaunch(intent)) return
        intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let(::handleCard)
    }

    override fun onStart() {
        super.onStart()
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        mPendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass),
            flags
        )
    }

    override fun onResume() {
        super.onResume()
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        val techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
            arrayOf(IsoDep::class.java.name, NfcA::class.java.name)
        )
        mNfcAdapter?.enableForegroundDispatch(this, mPendingIntent, filters, techLists)
        nfcTestBinding.nfcEnable.text = "当前NFC状态：${if (BuyerNFCUtil.Util.isNfcEnabled(this)) "已开启" else "未开启"}"
        nfcTestBinding.nfcSupport.text = "当前设备是否支持NFC：${if (BuyerNFCUtil.Util.isNfcSupported(this)) "支持" else "不支持"}"
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onDestroy() {
        cardExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun handleCard(tag: Tag) {
        val scanId = scanSequence.incrementAndGet()
        val detectedAt = SystemClock.elapsedRealtime()
        val selectedOperation = operation
        val url = nfcTestBinding.fm1208Url.text.toString().trim()
        val packageName = nfcTestBinding.fm1208Package.text.toString().trim()
        android.util.Log.i(
            LOG_TAG,
            "$LOG_PREFIX scan=$scanId handleCard operation=$selectedOperation, " +
                "uid=${tag.id.toHex()}, tech=${tag.techList.joinToString()}"
        )
        nfcTestBinding.nfcInfo.text = "FM1208 通信中...\nUID: ${tag.id.toHex()}"
        cardExecutor.execute {
            android.util.Log.i(
                LOG_TAG,
                "$LOG_PREFIX scan=$scanId task started operation=$selectedOperation, " +
                    "queueDelayMs=${SystemClock.elapsedRealtime() - detectedAt}"
            )
            val result = runCatching {
                when (selectedOperation) {
                    Operation.READ -> when (val content = Fm1208CardClient.read(tag)) {
                        is Fm1208CardClient.Content.NdefContent ->
                            "NDEF 读取成功 (${content.recordCount} records)\n" +
                                "URI: ${content.uri ?: "无"}\n包名: ${content.applicationPackage ?: "无"}"
                        is Fm1208CardClient.Content.FmcosContent -> {
                            val profile = content.profile
                            val applications = profile.applications.joinToString("\n") { entry ->
                                "#${entry.recordNumber} AID=${entry.aid ?: "无"} label=${entry.label ?: "无"}"
                            }.ifEmpty { "无已登记应用" }
                            "FMCOS 读取成功\nMF: ${profile.masterFileName ?: "未知"}\n" +
                                "DIR SFI: ${profile.directorySfi ?: "未知"}\n应用:\n$applications\n" +
                                "业务 URI: ${content.uri ?: "无"}\n" +
                                "业务包名: ${content.applicationPackage ?: "无"}\n" +
                                "业务 records: ${content.recordCount}"
                        }
                    }
                    Operation.WRITE_NDEF -> {
                        Fm1208CardClient.writeNdef(tag, url, packageName)
                        "NDEF 写入成功\nURI: $url\n包名: $packageName"
                    }
                    Operation.WRITE_FMCOS -> {
                        Fm1208CardClient.writeFmcos(tag, url, packageName)
                        "FMCOS 写入成功\nURI: $url\n包名: $packageName"
                    }
                    Operation.INITIALIZE_FMCOS_TEST -> {
                        val initialized = Fm1208CardClient.initializeFmcosTestCard(
                            tag,
                            url,
                            packageName
                        )
                        "FMCOS 测试卡初始化并校验成功\n" +
                            "完成步骤: ${initialized.completedSteps}\n" +
                            "业务数据: ${initialized.verifiedMessageBytes} bytes\n" +
                            "URI: $url\n包名: $packageName"
                    }
                }
            }.getOrElse { error ->
                android.util.Log.e(
                    LOG_TAG,
                    "$LOG_PREFIX scan=$scanId operation=$selectedOperation failed, " +
                        "error=${error.javaClass.simpleName}, message=${error.message}",
                    error
                )
                "操作失败: ${error.message ?: error.javaClass.simpleName}"
            }
            android.util.Log.i(
                LOG_TAG,
                "$LOG_PREFIX scan=$scanId task finished operation=$selectedOperation, " +
                    "elapsedMs=${SystemClock.elapsedRealtime() - detectedAt}"
            )
            runOnUiThread {
                nfcTestBinding.nfcRecord.text = result
                operation = Operation.READ
            }
        }
    }

    private fun ByteArray.toHex(): String = joinToString(":") { "%02X".format(it) }

    private fun redirectLegacyColdLaunch(sourceIntent: Intent): Boolean {
        if (sourceIntent.action != NfcAdapter.ACTION_TECH_DISCOVERED) return false
        val tag = sourceIntent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        android.util.Log.w(
            LOG_TAG,
            "$LOG_PREFIX legacy OEM NFC target received in onCreate, uid=${tag?.id?.toHex() ?: "unknown"}; " +
                "forwarding to cold-launch activity without opening TagTechnology"
        )
        startActivity(Intent(this, Fm1208NfcLaunchActivity::class.java).apply {
            action = NfcAdapter.ACTION_TECH_DISCOVERED
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            tag?.let { putExtra(NfcAdapter.EXTRA_TAG, it) }
        })
        finish()
        return true
    }

    private fun handleColdLaunch(intent: Intent): Boolean {
        if (intent.action != ACTION_FMCOS_COLD_LAUNCH) return false
        val uid = intent.getStringExtra(EXTRA_COLD_LAUNCH_UID) ?: "未知"
        android.util.Log.i(
            LOG_TAG,
            "$LOG_PREFIX cold launch handed off uid=$uid; waiting for foreground rediscovery"
        )
        nfcTestBinding.nfcInfo.text = "已通过 NFC 唤起 App\nUID: $uid"
        nfcTestBinding.nfcRecord.text =
            "为避免使用系统选择页面后失效的 Tag，请先移开卡片，再重新贴卡并保持到读取完成"
        operation = Operation.READ
        return true
    }

    private fun logOperationArmed(selectedOperation: Operation) {
        android.util.Log.i(LOG_TAG, "$LOG_PREFIX operation armed=$selectedOperation; waiting for a new tag intent")
    }

    private enum class Operation { READ, WRITE_NDEF, WRITE_FMCOS, INITIALIZE_FMCOS_TEST }

    companion object {
        private const val LOG_TAG = "Fm1208NfcActivity"
        private const val LOG_PREFIX = "[FM1208-APDU]"
        private const val ACTION_FMCOS_COLD_LAUNCH =
            "com.vdian.android.lib.testforgradle.action.FMCOS_COLD_LAUNCH"
        private const val EXTRA_COLD_LAUNCH_UID = "fm1208_cold_launch_uid"

        fun createColdLaunchIntent(context: Context, uid: String?): Intent =
            Intent(context, NFCIdentificationActivity::class.java).apply {
                action = ACTION_FMCOS_COLD_LAUNCH
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_COLD_LAUNCH_UID, uid)
            }
    }
}
