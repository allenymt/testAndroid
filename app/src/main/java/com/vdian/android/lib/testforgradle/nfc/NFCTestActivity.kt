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
import kotlinx.android.synthetic.main.activity_nfctest.view.nfc_info

class NFCTestActivity : AppCompatActivity() {
    private lateinit var nfcTestBinding: ActivityNfctestBinding

    private var mNfcAdapter: NfcAdapter? = null
    private lateinit var mPendingIntent: PendingIntent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcTestBinding = ActivityNfctestBinding.inflate(layoutInflater)
        setContentView(nfcTestBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcTestBinding.nfcInfo.text = BuyerNFCUtil.Companion.readNFCInfo(intent)
        nfcTestBinding.nfcRecord.text = BuyerNFCUtil.Companion.readNfcUri(intent).joinToString("\n\n")
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