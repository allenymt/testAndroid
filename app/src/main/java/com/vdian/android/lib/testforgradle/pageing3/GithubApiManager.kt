package com.vdian.android.lib.testforgradle.pageing3

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @author yulun
 * @since 2022-07-26 19:40
 */
object GithubApiManager {

    val githubServiceApi: PagingGithubService by lazy {
        val retrofit = retrofit2.Retrofit.Builder()
            .client(
                OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().setLevel(
                    HttpLoggingInterceptor.Level.BODY))
                .build())
            .baseUrl(PagingGithubService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(PagingGithubService::class.java)
    }
}