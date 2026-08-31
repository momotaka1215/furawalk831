package com.momo.furawalk.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 出現中の歩荷さんイベント情報を保持するエンティティ
 */
@Entity(tableName = "bokka_events")
data class BokkaEventEntity(
    @PrimaryKey val eventId: String,
    val spawnTime: Long,        // 出現時刻（サーバー基準）
    val expireTime: Long,       // 消滅時刻（サーバー基準）
    val latitude: Double,
    val longitude: Double,
    val spotName: String,       // 出現場所のPOI名称
    val bokkaType: String,      // 旅、山、花、など
    val message: String,        // 出現時のセリフ
    val isActive: Boolean = true,
    val isArrived: Boolean = false // 到着判定済みフラグ
)

/**
 * 歩荷さんがその時持っている商品の在庫情報を保持するエンティティ
 */
@Entity(tableName = "bokka_inventory")
data class BokkaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: String,
    val itemId: String,
    val name: String,
    val price: Long,
    var stock: Int
)
