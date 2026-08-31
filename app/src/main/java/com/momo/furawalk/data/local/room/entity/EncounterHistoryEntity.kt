package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encounter_history")
data class EncounterHistoryEntity(
    @PrimaryKey(autoGenerate = true) val encounterId: Long = 0,
    val peerHashedId: String,
    val peerName: String,
    val peerLevel: Int = 1,
    val peerTotalDistance: Double = 0.0,
    val greetingMessage: String = "こんにちは！",
    val avatarPath: String? = null,
    val metAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null
)
