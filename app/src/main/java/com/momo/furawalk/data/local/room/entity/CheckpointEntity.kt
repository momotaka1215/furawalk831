package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.momo.furawalk.core.domain.model.map.Availability
import com.momo.furawalk.core.domain.model.map.Checkpoint
import com.momo.furawalk.core.domain.model.map.CheckpointType
import com.momo.furawalk.core.domain.model.map.Rewards

/**
 * データベースに保存されるチェックポイントのテーブル定義
 */
@Entity(tableName = "checkpoints")
data class CheckpointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeter: Float,
    val type: String,
    val priority: Int = 3, // 追加
    val isNightSafe: Boolean = true,
    val isWinterAccessible: Boolean = true,
    val expReward: Int,
    val moneyReward: Int,
    val itemIdReward: String?,
    val lastCalibratedAt: Long? = null // 追加: 補正日時
) {
    fun toDomain(): Checkpoint = Checkpoint(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeter = radiusMeter,
        type = try { 
            CheckpointType.valueOf(type) 
        } catch (e: Exception) { 
            // 互換性のため
            if (type == "RELIGIOUS") CheckpointType.SHRINE else CheckpointType.SIGHTSEEING 
        },
        priority = priority,
        availability = Availability(isNightSafe, isWinterAccessible),
        rewards = Rewards(expReward, moneyReward, itemIdReward)
    )

    companion object {
        fun fromDomain(domain: Checkpoint): CheckpointEntity = CheckpointEntity(
            id = domain.id,
            name = domain.name,
            latitude = domain.latitude,
            longitude = domain.longitude,
            radiusMeter = domain.radiusMeter,
            type = domain.type.name,
            priority = domain.priority,
            isNightSafe = domain.availability.nightSafe,
            isWinterAccessible = domain.availability.winterAccessible,
            expReward = domain.rewards.exp,
            moneyReward = domain.rewards.money,
            itemIdReward = domain.rewards.itemId,
            lastCalibratedAt = null // ドメインモデルから変換時は一旦null（Repositoryで保護される）
        )
    }
}
