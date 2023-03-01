package com.vdian.android.lib.testforgradle.pageing3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow

/**
 * @author yulun
 * @since 2022-07-26 19:49
 */
class PagingTestModel : ViewModel() {

    fun getPagingData(): Flow<PagingData<PagingRepositoryItem>> {

        return Repository.getGithubPagingData().cachedIn(viewModelScope)
    }
}

