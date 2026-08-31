package com.momo.furawalk.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("bonus_heso") val bonusHeso: Int,
    @SerialName("bonus_exp") val bonusExp: Int,
    @SerialName("reward_item_id") val rewardItemId: String? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("icon_emoji") val iconEmoji: String,
    @SerialName("target_checkpoint_id") val targetCheckpointId: String? = null,
    @SerialName("condition_type") val conditionType: String = "LOCATION",
    @SerialName("condition_value") val conditionValue: Long = 0
)
