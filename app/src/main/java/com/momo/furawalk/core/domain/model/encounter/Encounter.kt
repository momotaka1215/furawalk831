package com.momo.furawalk.core.domain.model.encounter

/**
 * すれ違い通信で交換するデータモデル
 */
data class Encounter(
    val userId: String,
    val avatarParts: AvatarParts,
    val messageId: Int,
    val timestamp: Long
)

data class AvatarParts(
    val hairId: Int,
    val clothesId: Int,
    val hatId: Int,
    val weaponId: Int
)
