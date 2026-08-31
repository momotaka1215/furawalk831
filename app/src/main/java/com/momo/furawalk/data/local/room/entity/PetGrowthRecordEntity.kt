package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_growth_records")
data class PetGrowthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petId: String,
    val message: String,
    val type: String, // "LEVEL", "HEIGHT", "FRIENDSHIP", "INTELLIGENCE", "STAMINA"
    val createdAt: Long = System.currentTimeMillis(),
    val isShown: Boolean = false
)
