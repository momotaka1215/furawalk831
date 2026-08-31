package com.momo.furawalk.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ShopItemDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Long,
    val sellPrice: Long = 0, // 追加
    val imageUrl: String,
    val category: String = "OTHER",
    val shopType: String = "FIXED", // FIXED, PEDDLER
    val meritText: String = "",
    val demeritText: String = "",
    val effects: List<ItemEffectDto> = emptyList(),
    val favoriteSpecies: String? = null,
    val favoriteBonusEffects: List<ItemEffectDto> = emptyList(),
    val isLimited: Boolean = false
)

@Serializable
data class ItemEffectDto(
    val type: String, // HUNGER, HAPPINESS, HEALTH, FATIGUE, CLEANLINESS, FRIENDSHIP, AFFECTION, HEIGHT, WEIGHT, BODY_TYPE, INTELLIGENCE, STAMINA, EXP
    val value: Float
)

@Serializable
data class ShopResponseDto(
    val items: List<ShopItemDto> = emptyList(),
    val checkInRewardConfig: CheckInRewardConfigDto? = null, // 追加
    val basicItems: List<ShopItemDto> = emptyList(),
    val limitedItems: List<ShopItemDto> = emptyList()
)
