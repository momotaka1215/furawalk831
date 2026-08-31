package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avatar_master")
data class AvatarEntity(
    @PrimaryKey val id: String,
    val name: String,
    val remoteUrl: String,
    val localPath: String? = null,
    val isDownloaded: Boolean = false
)
