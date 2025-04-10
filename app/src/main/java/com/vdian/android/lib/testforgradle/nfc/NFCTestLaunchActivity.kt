package com.vdian.android.lib.testforgradle.nfc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivityNfcLaunchBinding


class NFCTestLaunchActivity : AppCompatActivity() {
    private lateinit var nfcTestBinding: ActivityNfcLaunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcTestBinding = ActivityNfcLaunchBinding.inflate(layoutInflater)
        setContentView(nfcTestBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        nfcTestBinding.nfcInfo.text = "NFCTestLaunchActivity-NFC Info"
        android.util.Log.i("BuyerNFCUtil", "NFCTestLaunchActivity onCreate: $intent")
        var nfcInfo = BuyerNFCUtil.Reader.readNFCInfo(intent)
        nfcTestBinding.nfcInfo.text = nfcInfo
        nfcTestBinding.nfcRecord.text = BuyerNFCUtil.Reader.readNfcUri(intent).joinToString("\n\n")
        if (nfcInfo != null) {
            startActivity(Intent(this, NFCEmptyActivity::class.java).apply {
                putExtra("nfcInfo", nfcInfo)
            })
            finish()
        }
    }
}