package com.momo.furawalk.data.remote.api

import okhttp3.ResponseBody

/**
 * ネットワーク通信を担当するインターフェース
 */
interface WorldApi {
    /**
     * 富良野の目的地データを取得する
     * @param url 同期先URL
     */
    suspend fun fetchCheckpoints(url: String): ResponseBody

    /**
     * ショップの商品データを取得する
     * @param url ショップデータURL
     */
    suspend fun fetchShopData(url: String): ResponseBody

    /**
     * ペットの種族データを取得する
     * @param url ペットデータURL
     */
    suspend fun fetchPetData(url: String): ResponseBody

    /**
     * イベントデータを取得する
     * @param url イベントデータURL
     */
    suspend fun fetchEventData(url: String): ResponseBody
}
