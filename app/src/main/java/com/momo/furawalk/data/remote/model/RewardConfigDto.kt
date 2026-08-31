package com.momo.furawalk.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class RewardConfigDto(
    val checkInRewardConfig: CheckInRewardConfigDto
)

@Serializable
data class CheckInRewardConfigDto(
    val probabilityGroups: List<RewardGroupDto>
)

@Serializable
data class RewardGroupDto(
    val name: String,
    val chance: Double, // パーセンテージ (0.01 = 0.01%)
    val itemIds: List<String>
)
