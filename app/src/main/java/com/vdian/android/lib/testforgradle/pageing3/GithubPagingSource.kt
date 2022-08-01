package com.vdian.android.lib.testforgradle.pageing3

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * @author yulun
 * @since 2022-07-26 19:41
 */
class GithubPagingSource: PagingSource<Int, PagingRepositoryItem>() {

    override fun getRefreshKey(state: PagingState<Int, PagingRepositoryItem>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PagingRepositoryItem> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize
            val rspRepository = GithubApiManager.githubServiceApi.getRepositories(page, pageSize)
            val items = rspRepository.items
            val preKey = if (page > 1) page - 1 else null
            val nextKey = if (items.isNotEmpty()) page + 1 else null
            LoadResult.Page(items, preKey, nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}