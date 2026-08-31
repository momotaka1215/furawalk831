package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_catalog")
data class ShopItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Long,
    val sellPrice: Long = 0, // 追加
    val category: String = "OTHER",
    val shopType: String = "FIXED", // FIXED, PEDDLER
    val meritText: String = "",
    val demeritText: String = "",
    val effectsJson: String = "",
    val favoriteSpecies: String? = null,
    val favoriteBonusEffectsJson: String = "",
    val remoteImageUrl: String,
    val localImagePath: String? = null,
    val isImageDownloaded: Boolean = false,
    val isLimited: Boolean = false,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
