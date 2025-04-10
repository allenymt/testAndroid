package com.vdian.android.lib.testforgradle.nfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivityNfcEmptyBinding
import com.vdian.android.lib.testforgradle.databinding.ActivityNfcSingleTaskBinding
import com.vdian.android.lib.testforgradle.databinding.ActivityNfctestBinding


class NFCEmpty2Activity : AppCompatActivity() {
    private lateinit var nfcTestBinding: ActivityNfcEmptyBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcTestBinding = ActivityNfcEmptyBinding.inflate(layoutInflater)
        setContentView(nfcTestBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        nfcTestBinding.title.text = "NFCEmpty2Activity"
        nfcTestBinding.goNextPage.setOnClickListener {
            val intent = Intent(this, NFCSingleTaskActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }


    override fun onStart() {
        super.onStart()

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}