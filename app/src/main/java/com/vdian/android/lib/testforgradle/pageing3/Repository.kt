package com.vdian.android.lib.testforgradle.pageing3

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * @author yulun
 * @since 2022-07-26 19:45
 */
object Repository {

    private const val PAGE_SIZE = 25

    fun getGithubPagingData(): Flow<PagingData<PagingRepositoryItem>> {
        return Pager(
            config = PagingConfig(PAGE_SIZE),
            pagingSourceFactory = {GithubPagingSource()}
        ).flow
    }
}