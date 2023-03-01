package com.vdian.android.lib.testforgradle.room

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vdian.android.lib.testforgradle.R
import com.vdian.android.lib.testforgradle.databinding.ActivityRoomTestBinding
import kotlinx.coroutines.launch

class RoomTestActivity : AppCompatActivity() {
    private var _binding: ActivityRoomTestBinding? = null
    private val binding get() = _binding!!
    private lateinit var wordDao: WordDao
    private lateinit var wordAdapter: WordAdapter
    private var mList: MutableList<WordEntity> =  mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityRoomTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wordDao = WordDB.getInstance(this)?.getWordDao() ?: return
        wordAdapter = WordAdapter(this,mList)
        binding.recyclerview?.adapter = wordAdapter
        binding.recyclerview?.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            mList.addAll(wordDao.queryAll())
            wordAdapter.notifyDataSetChanged()
        }

        binding.btnSave?.setOnClickListener {
            lifecycleScope.launch {
                binding.et?.text.toString().let {
                    wordDao.insert(WordEntity(it, content = "content:+${it}"))
                    updateData()
                }
            }
        }

        binding.btnDelete?.setOnClickListener {
            lifecycleScope.launch {
                wordDao.deleteAll()
                updateData()
            }
        }

        wordAdapter.setWordDeleteListener(object :WordAdapter.WordDeleteListener{
            override fun delete(id: Long) {
                lifecycleScope.launch {
                    wordDao.deleteById(id)
                    updateData()
                }
            }
        })
    }

    fun updateData(){
        mList.clear()
        mList.addAll(wordDao.queryAll())
        wordAdapter.notifyDataSetChanged()
    }
}