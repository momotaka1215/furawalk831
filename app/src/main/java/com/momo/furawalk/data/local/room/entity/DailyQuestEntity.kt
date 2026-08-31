package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_quests")
data class DailyQuestEntity(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val checkpointId: String,
    val isCompleted: Boolean = false,
    val isRewarded: Boolean = false
)
