package com.vdian.android.lib.testforgradle.rom

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vdian.android.lib.testforgradle.R

class RomCheckActivity : AppCompatActivity() {
    val TAG = "RomCheck"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rom_check)
    }

    fun isMIUI(view: View) {
        android.util.Log.i(TAG, "isMIUI ${RomChecker.isMiui()}")
    }


}