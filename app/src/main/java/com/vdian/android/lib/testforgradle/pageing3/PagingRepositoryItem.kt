package com.vdian.android.lib.testforgradle.pageing3

import com.google.gson.annotations.SerializedName

/**
 * @author yulun
 * @since 2022-07-26 19:29
 */
data class PagingRepositoryItem(
    @SerializedName("id") var id: Int,
    @SerializedName("name") var name: String,
    @SerializedName("html_url") var htmlUrl: String,
    @SerializedName("description") var description: String,
    @SerializedName("stargazers_count") var stargazersCount: Int,
)

