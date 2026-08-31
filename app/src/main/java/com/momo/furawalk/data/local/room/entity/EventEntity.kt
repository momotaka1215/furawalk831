package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val bonusHeso: Int,
    val bonusExp: Int,
    val rewardItemId: String?,
    val startDate: String? = null,
    val endDate: String? = null,
    val iconEmoji: String,
    val targetCheckpointId: String? = null,
    val conditionType: String = "LOCATION",
    val conditionValue: Long = 0
)
