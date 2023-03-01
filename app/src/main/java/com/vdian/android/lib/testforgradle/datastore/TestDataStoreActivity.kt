package com.vdian.android.lib.testforgradle.datastore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vdian.android.lib.testforgradle.databinding.ActivityTestDataStoreBinding
import kotlinx.coroutines.launch

class TestDataStoreActivity : AppCompatActivity() {

    private var _binding: ActivityTestDataStoreBinding? = null
    private val binding get() = _binding!!
    val key1 = "key1"
    val key2 = "key2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTestDataStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.read.setOnClickListener {
            lifecycleScope.launch {
                var  sss = DataStoreManager.read(key1, this@TestDataStoreActivity)
                android.util.Log.d("TestDataStoreActivity", "read $sss")

                var  sss2 = DataStoreManager.read(key2, this@TestDataStoreActivity)
                android.util.Log.d("TestDataStoreActivity", "read $sss2")
            }
        }

        binding.write.setOnClickListener {
            lifecycleScope.launch {
                DataStoreManager.write(key1, "123*sadf",this@TestDataStoreActivity)

                var  sss2 = DataStoreManager.write(key2, "*456*123123",this@TestDataStoreActivity)
                android.util.Log.d("TestDataStoreActivity", "write $sss2")
            }

        }
    }
}