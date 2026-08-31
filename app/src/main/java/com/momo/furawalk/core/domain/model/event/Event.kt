package com.momo.furawalk.core.domain.model.event

/**
 * 期間限定イベントや特別クエストを表すドメインモデル
 */
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val bonusHeso: Int,
    val bonusExp: Int,
    val rewardItemId: String? = null,
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String,   // "YYYY-MM-DD"
    val iconEmoji: String,
    val targetCheckpointId: String? = null,
    val conditionType: String = "LOCATION", // LOCATION, DISTANCE, STEPS, CHECKIN_COUNT
    val conditionValue: Long = 0,
    val isCompleted: Boolean = false,
    val isRewarded: Boolean = false
)
