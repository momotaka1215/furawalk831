package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val date: String, // "YYYY-MM-DD"形式
    val sessionDurationMillis: Long = 0,
    val distanceMeters: Double = 0.0,
    val steps: Int = 0,
    val pointsEarned: Long = 0,
    val moneyEarned: Int = 0
)
