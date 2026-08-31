package com.momo.furawalk.core.domain.model.pet

/**
 * ペットの基本データ型
 */
data class Pet(
    val id: String,
    val name: String,
    val speciesId: String,
    val level: Int,
    val experience: Long,
    val hunger: Float,     // 0.0 to 1.0
    val happiness: Float,  // 0.0 to 1.0
    val avatarParts: PetAvatar
)

data class PetAvatar(
    val bodyId: String,
    val accessoryId: String? = null,
    val colorId: String
)
