package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerEntity(
    @PrimaryKey val id: String = "default_player",
    val name: String,
    val birthDate: Long,
    val gender: String = "unknown",
    
    // --- 累計アクティビティ記録 ---
    val totalSteps: Long = 0,
    val totalDistance: Double = 0.0,
    val totalPlayTimeMillis: Long = 0,
    val totalEncounters: Int = 0,
    val uniquePlayersMet: Int = 0,
    val totalCheckIns: Int = 0,
    
    // --- RPG要素: 職業・アビリティ ---
    val jobId: String = "novice",
    val playerLevel: Int = 1,
    val playerExp: Long = 0,
    
    // 基本パラメータ (10-100)
    val str: Int = 10, // 筋力
    val agi: Int = 10, // 敏捷
    val sta: Int = 10, // 体力
    val int: Int = 10, // 知能
    val luk: Int = 10, // 幸運
    val cha: Int = 10, // 魅力
    
    val availableAbilityPoints: Int = 0,

    // --- 設定・状態 ---
    val greetingMessage: String = "こんにちは！",
    val avatarPath: String? = null,
    val currentTitleId: String? = null,
    val activeCheckpointId: String? = null,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val lastSelectionTime: Long? = null,
    val isDistanceBonusInvalidated: Boolean = false,
    val lastLoginAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
