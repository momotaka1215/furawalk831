package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_currencies")
data class PlayerCurrencyEntity(
    @PrimaryKey val type: String, // MONEY, EXP, POINT
    val currentAmount: Long = 0,
    val totalEarned: Long = 0,
    val totalSpent: Long = 0
)
