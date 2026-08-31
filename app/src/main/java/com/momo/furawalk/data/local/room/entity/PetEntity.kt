package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ペットの個体情報を保持するエンティティ
 */
@Entity(tableName = "pet_status")
data class PetEntity(
    @PrimaryKey val id: String = "current_pet",
    val name: String,
    val speciesId: String,
    val formId: String = "normal",
    
    // 基本ステータス (0.0 - 1.0 または数値)
    val level: Int = 1,
    val experience: Long = 0,
    val health: Float = 1.0f,
    val hunger: Float = 1.0f,
    val happiness: Float = 1.0f,
    val friendship: Float = 0.0f,
    val fatigue: Float = 0.0f,      // 疲労 (0.0:なし - 1.0:限界)
    val cleanliness: Float = 1.0f,  // 清潔 (0.0:汚い - 1.0:綺麗)
    
    // 身体的特徴
    val height: Float = 25.0f,     // 身長 (cm)
    val weight: Float = 1.5f,      // 体重 (kg)
    val bodyType: Float = 50.0f,    // 体型 (0:やせすぎ - 100:ふっくら)
    
    // 能力値
    val intelligence: Int = 10,
    val stamina: Int = 10,
    
    // 性格要素 (0 - 100)
    val activity: Int = 50,         // 活発さ
    val affection: Int = 50,        // 甘えん坊度
    val bravery: Int = 50,          // 勇敢さ
    val gentleness: Int = 50,       // おっとり度
    
    // 生まれ持った特性 (hidden)
    val innateHeightTrend: Float = 1.0f,    // 身長の伸びやすさ (係数)
    val innateBodyTrend: Float = 1.0f,      // 太りやすさ (係数)
    val innateIntelligenceTrend: Float = 1.0f,
    val innateStaminaTrend: Float = 1.0f,
    
    // 遺伝・血統情報
    val dnaSeed: Long = 0,
    val generation: Int = 1,
    val parentId: String? = null,
    val lineageData: String? = null, // 祖先の記録 (JSON形式)
    
    // レアリティ・突然変異
    val rarityTier: Int = 0,         // 0:通常, 1:珍しい, 2:レア ...
    val isMutant: Boolean = false,
    
    val customImageUri: String? = null, // 追加: ユーザー撮影・選択画像
    
    val favoriteColor: String? = null, // 追加
    val favoriteFood: String? = null,  // 追加
    
    val dailyStrokeCount: Int = 0,     // 追加: 1日のなでる回数
    val dailyPlayCount: Int = 0,       // 追加: 1日の遊ぶ回数
    val isResting: Boolean = false,    // 追加: 休息状態フラグ
    val lastActionResetDate: String? = null, // 追加: 最後にリセットした日付
    
    val walkDistanceCounter: Float = 0.0f, // 追加: 100mごとのステータス変動用カウンター
    val lastUpdateAt: Long = System.currentTimeMillis()
) {
    /**
     * 健康状態を文字列で取得
     */
    fun getHealthStatus(): String {
        return when {
            health <= 0.1f -> "弱っている"
            health <= 0.3f -> "病気"
            health <= 0.5f -> "元気がない"
            else -> "元気"
        }
    }
}
