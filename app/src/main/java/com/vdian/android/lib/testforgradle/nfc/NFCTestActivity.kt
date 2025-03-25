package com.vdian.android.lib.testforgradle.nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivityNfctestBinding


class NFCTestActivity : AppCompatActivity() {
    private lateinit var nfcTestBinding: ActivityNfctestBinding

    private var mNfcAdapter: NfcAdapter? = null
    private lateinit var mPendingIntent: PendingIntent
    private var id = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcTestBinding = ActivityNfctestBinding.inflate(layoutInflater)
        setContentView(nfcTestBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        nfcTestBinding.nfcButton.setOnClickListener {
//            mNfcAdapter?.let {
//                BuyerNFCUtil.Writer.writeNfcData(this, intent, "https://www.baidu.com?id=${id++}",)
//            }
//        }
        nfcTestBinding.root.postDelayed({
            android.util.Log.i("BuyerNFCUtil", "test onCreate write")
            BuyerNFCUtil.Writer.writeNfcData(this, intent, "https://wdb-applink.weidian.com?id=${id++}")
        }, 2000)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.i("BuyerNFCUtil", "onNewIntent: $intent")
        setIntent(intent)
        nfcTestBinding.nfcInfo.text = BuyerNFCUtil.Reader.readNFCInfo(intent)
        nfcTestBinding.nfcRecord.text = BuyerNFCUtil.Reader.readNfcUri(intent).joinToString("\n\n")
        // nfcTestBinding.root.postDelayed({
        //     android.util.Log.i("NFCTestActivity", "test onNewIntent write")
        //     BuyerNFCUtil.Writer.writeNfcData(this, intent, "https://www.baidu.com?id=${id++}",)
        // }, 5000)
        
        android.util.Log.i("BuyerNFCUtil", "test onNewIntent write")
        BuyerNFCUtil.Writer.writeNfcData(this, intent, "https://wdb-applink.weidian.com?id=${id++}")
}

    override fun onStart() {
        super.onStart()
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        mPendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter?.enableForegroundDispatch(this, mPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableForegroundDispatch(this)
    }
}