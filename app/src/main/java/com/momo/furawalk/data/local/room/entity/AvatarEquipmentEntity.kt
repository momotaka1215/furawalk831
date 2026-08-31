package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avatar_equipment")
data class AvatarEquipmentEntity(
    @PrimaryKey val slotId: String, // HEAD, BODY, BACK, etc.
    val partId: String
)
