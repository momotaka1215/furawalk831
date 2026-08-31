package com.momo.furawalk.data.local.room.dao

import androidx.room.*
import com.momo.furawalk.data.local.room.entity.BokkaEventEntity
import com.momo.furawalk.data.local.room.entity.BokkaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BokkaDao {
    // アクティブかつ期限内のイベントを取得
    @Query("SELECT * FROM bokka_events WHERE isActive = 1 AND expireTime > :currentTime LIMIT 1")
    fun getActiveBokkaEvent(currentTime: Long): Flow<BokkaEventEntity?>

    // 指定されたイベントの在庫リストを取得
    @Query("SELECT * FROM bokka_inventory WHERE eventId = :eventId")
    fun getBokkaInventory(eventId: String): Flow<List<BokkaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBokkaEvent(event: BokkaEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBokkaInventory(items: List<BokkaItemEntity>)

    @Update
    suspend fun updateBokkaEvent(event: BokkaEventEntity)

    // 在庫を減らす (セキュリティ上、Dao側でトランザクション管理)
    @Transaction
    suspend fun purchaseItem(eventId: String, itemId: String): Boolean {
        val item = getItemByEventAndId(eventId, itemId)
        if (item != null && item.stock > 0) {
            updateStock(item.id, item.stock - 1)
            return true
        }
        return false
    }

    @Query("SELECT * FROM bokka_inventory WHERE eventId = :eventId AND itemId = :itemId LIMIT 1")
    suspend fun getItemByEventAndId(eventId: String, itemId: String): BokkaItemEntity?

    @Query("UPDATE bokka_inventory SET stock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Int, newStock: Int)

    @Query("UPDATE bokka_events SET isActive = 0 WHERE expireTime <= :currentTime")
    suspend fun deactivateExpiredEvents(currentTime: Long)

    @Query("DELETE FROM bokka_events WHERE isActive = 0")
    suspend fun cleanupInactiveEvents()
}
