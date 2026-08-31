package com.momo.furawalk.core.engine.logic

import android.content.res.AssetManager
import android.util.Log
import com.momo.furawalk.data.local.room.dao.ShopDao
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import com.momo.furawalk.data.remote.api.WorldApi
import com.momo.furawalk.data.remote.model.ShopItemDto
import com.momo.furawalk.data.remote.model.ShopResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ShopSyncManager(
    private val worldApi: WorldApi,
    private val shopDao: ShopDao,
    private val itemImageDir: File,
    private val assetManager: AssetManager? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * アセット内の定義ファイルから初期データをロードする
     */
    suspend fun loadItemsFromAssets(onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (assetManager == null) return@withContext
        
        val files = listOf("shop_items.json", "items.json")
        
        for (fileName in files) {
            try {
                withContext(Dispatchers.Main) { onProgress(fileName) }
                val rawData = assetManager.open(fileName).bufferedReader().use { it.readText() }
                val cleanJson = sanitizeJson(rawData)
                val shopResponse = json.decodeFromString<ShopResponseDto>(cleanJson)
                
                for (dto in shopResponse.items) {
                    processShopItem(dto)
                }
                Log.d("ShopSyncManager", "Loaded items from assets: $fileName")
            } catch (e: Exception) {
                val errorMsg = "Failed to load items from assets ($fileName): ${e.message}"
                Log.e("ShopSyncManager", errorMsg)
                println("ShopSyncManager Error: $errorMsg")
                e.printStackTrace()
            }
        }
    }

    suspend fun syncShopData(url: String, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!itemImageDir.exists()) itemImageDir.mkdirs()

        try {
            val fileName = url.substringAfterLast("/")
            withContext(Dispatchers.Main) { onProgress(fileName) }
            Log.d("ShopSyncManager", "Fetching shop data from: $url")
            val responseBody = worldApi.fetchShopData(url)
            val rawData = responseBody.string()
            val cleanJson = sanitizeJson(rawData)
            
            val shopResponse = json.decodeFromString<ShopResponseDto>(cleanJson)
            
            val allItems = mutableListOf<ShopItemDto>()
            // 新しい JSON 形式 (items) と古い形式 (basicItems/limitedItems) 両方に対応
            if (shopResponse.items.isNotEmpty()) {
                allItems.addAll(shopResponse.items)
            } else {
                allItems.addAll(shopResponse.basicItems.map { it.copy(isLimited = false) })
                allItems.addAll(shopResponse.limitedItems.map { it.copy(isLimited = true) })
            }

            for (dto in allItems) {
                processShopItem(dto)
            }
            
            Log.d("ShopSyncManager", "Shop sync complete. Items: ${allItems.size}")
        } catch (e: Exception) {
            val errorMsg = "Shop sync failed for $url: ${e.message}"
            Log.e("ShopSyncManager", errorMsg)
            println("ShopSyncManager Error: $errorMsg")
            e.printStackTrace()
        }
    }

    private suspend fun processShopItem(dto: ShopItemDto) {
        val existing = shopDao.getItemById(dto.id)
        
        // 差分チェック: 基本データが同じで、かつ画像がDL済みならスキップ
        val isSameData = existing != null && 
                existing.name == dto.name && 
                existing.price == dto.price && 
                existing.remoteImageUrl == dto.imageUrl &&
                existing.category == dto.category &&
                existing.shopType == dto.shopType &&
                existing.meritText == dto.meritText &&
                existing.demeritText == dto.demeritText
        
        val fileName = "item_${dto.id}_${dto.imageUrl.substringAfterLast("/")}"
        val localFile = File(itemImageDir, fileName)
        
        var localPath: String? = existing?.localImagePath
        var isDownloaded = existing?.isImageDownloaded ?: false

        // 画像のURLが変わったか、ファイルが存在しない場合は再ダウンロード
        if (existing?.remoteImageUrl != dto.imageUrl || !localFile.exists() || !isDownloaded) {
            try {
                downloadFile(dto.imageUrl, localFile)
                localPath = localFile.absolutePath
                isDownloaded = true
                Log.d("ShopSyncManager", "Downloaded/Updated item image: ${dto.name}")
            } catch (e: Exception) {
                Log.e("ShopSyncManager", "Failed to download image for ${dto.name}: ${e.message}")
            }
        } else if (isSameData) {
            // データも画像も同じならDB更新をスキップして負荷を減らす
            return
        }

        val entity = ShopItemEntity(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            price = dto.price,
            sellPrice = dto.sellPrice,
            category = dto.category,
            shopType = dto.shopType,
            meritText = dto.meritText,
            demeritText = dto.demeritText,
            effectsJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.momo.furawalk.data.remote.model.ItemEffectDto.serializer()), dto.effects),
            favoriteSpecies = dto.favoriteSpecies,
            favoriteBonusEffectsJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.momo.furawalk.data.remote.model.ItemEffectDto.serializer()), dto.favoriteBonusEffects),
            remoteImageUrl = dto.imageUrl,
            localImagePath = localPath,
            isImageDownloaded = isDownloaded,
            isLimited = dto.isLimited,
            lastUpdatedAt = System.currentTimeMillis()
        )
        shopDao.insertOrUpdateItem(entity)
    }

    private fun downloadFile(urlStr: String, targetFile: File) {
        val url = URL(urlStr)
        url.openStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun sanitizeJson(rawData: String): String {
        var json = rawData.trim()
        if (json.startsWith("//")) {
            json = json.substringAfter("\n")
        }
        return json.replace("\uFEFF", "").trim()
    }
}
