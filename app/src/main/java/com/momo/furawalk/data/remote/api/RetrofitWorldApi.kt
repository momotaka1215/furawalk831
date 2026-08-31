package com.momo.furawalk.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Retrofitを使用したWorldApiの実装
 */
interface RetrofitWorldApi : WorldApi {
    @GET
    override suspend fun fetchCheckpoints(@Url url: String): ResponseBody

    @GET
    override suspend fun fetchShopData(@Url url: String): ResponseBody

    @GET
    override suspend fun fetchPetData(@Url url: String): ResponseBody

    @GET
    override suspend fun fetchEventData(@Url url: String): ResponseBody
}
