package com.vdian.android.lib.testforgradle.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log

/** Manifest NFC entry point. It never opens a TagTechnology connection. */
class Fm1208NfcLaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val uid = tag?.id?.toHex()
        Log.i(
            LOG_TAG,
            "$LOG_PREFIX cold launch action=${intent.action}, hasTag=${tag != null}, uid=${uid ?: "unknown"}; " +
                "discarding initial Tag handle"
        )
        startActivity(NFCIdentificationActivity.createColdLaunchIntent(this, uid))
        finish()
    }

    private fun ByteArray.toHex(): String = joinToString(":") { "%02X".format(it) }

    companion object {
        private const val LOG_TAG = "Fm1208NfcLaunch"
        private const val LOG_PREFIX = "[FM1208-APDU]"
    }
}
