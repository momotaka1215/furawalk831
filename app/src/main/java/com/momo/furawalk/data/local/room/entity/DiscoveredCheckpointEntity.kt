package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_checkpoints")
data class DiscoveredCheckpointEntity(
    @PrimaryKey val checkpointId: String,
    val firstVisitedAt: Long = System.currentTimeMillis(),
    val lastVisitedAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)
