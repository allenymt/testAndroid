package com.vdian.android.lib.testforgradle.pageing3

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.vdian.android.lib.testforgradle.databinding.ActivityPagingTestBinding
import kotlinx.coroutines.launch

class PagingTestActivity : AppCompatActivity() {
    private var _binding: ActivityPagingTestBinding? = null
    private val binding get() = _binding!!

    private val mAdapter = RepositoryAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPagingTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mainViewModel = ViewModelProvider(this).get(PagingTestModel::class.java)
        val recyclerView = binding.recyclerView
        val progressBar = binding.progressBar
        val refreshLayout = binding.refreshLayout
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = mAdapter
        lifecycleScope.launch {
            mainViewModel.getPagingData().collect { pagingData ->
                mAdapter.submitData(pagingData)
            }
        }
        mAdapter.addLoadStateListener {
            when (it.refresh) {
                is LoadState.NotLoading -> {
                    progressBar?.visibility = View.INVISIBLE
                    recyclerView?.visibility = View.VISIBLE
                    refreshLayout?.isRefreshing = false

                }
                is LoadState.Loading -> {
                    refreshLayout?.isRefreshing = true
                    progressBar?.visibility = View.VISIBLE
                    recyclerView?.visibility = View.INVISIBLE
                }
                is LoadState.Error -> {
                    progressBar?.visibility = View.INVISIBLE
                    refreshLayout?.isRefreshing = false
                }
            }
        }

        refreshLayout?.setOnRefreshListener {
            recyclerView?.swapAdapter(mAdapter, true)
            mAdapter.refresh()
        }
    }
}