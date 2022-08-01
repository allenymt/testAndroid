package com.vdian.android.lib.testforgradle.pageing3

import com.google.gson.annotations.SerializedName

/**
 * @author yulun
 * @since 2022-07-26 19:33
 */
class RspRepository {

    @SerializedName("total_count")
    var totalCount: Int = 0
    @SerializedName("incomplete_results")
    var incompleteResults: Boolean = false
    @SerializedName("items")
    var items: List<PagingRepositoryItem> = emptyList()
}