package com.vdian.android.lib.testforgradle.pageing3

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author yulun
 * @since 2022-07-26 19:36
 */
interface PagingGithubService {
    companion object{
        const val BASE_URL = "https://api.github.com/"
        const val REPO_LIST = "search/repositories?sort=stars&q=Android"
    }

    @GET(REPO_LIST)
    suspend fun getRepositories(@Query("page") page: Int, @Query("per_page") perPage: Int): RspRepository

}