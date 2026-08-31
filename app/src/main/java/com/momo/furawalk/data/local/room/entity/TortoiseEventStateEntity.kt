package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 「アキレスと亀」イベントの進行状態を保持するエンティティ
 */
@Entity(tableName = "tortoise_event_state")
data class TortoiseEventStateEntity(
    @PrimaryKey val eventId: String = "achilles_and_tortoise",
    val state: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, FAILED, ESCAPED
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val currentStageIndex: Int = 0, // 0: 小, 1: 中, 2: 大
    val currentDestinationId: String? = null,
    val currentDestinationName: String? = null,
    val currentDestinationLatitude: Double? = null,
    val currentDestinationLongitude: Double? = null,
    val destinationCreatedAt: Long = 0,
    val destinationDeadline: Long = 0,
    val escapeCount: Int = 0,
    val earnedHeso: Int = 0,
    val startedAt: Long = 0
)
