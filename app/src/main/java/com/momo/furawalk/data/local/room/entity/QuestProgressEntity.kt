package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quest_progress")
data class QuestProgressEntity(
    @PrimaryKey val questId: String,
    val status: String, // ACTIVE, COMPLETED, REWARDED
    val progressValue: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
