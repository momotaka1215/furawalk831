package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val itemId: String,
    val quantity: Int = 0,
    val acquiredAt: Long = System.currentTimeMillis()
)
