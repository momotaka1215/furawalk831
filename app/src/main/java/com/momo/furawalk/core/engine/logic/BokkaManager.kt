package com.momo.furawalk.core.engine.logic

import android.location.Location
import android.util.Log
import com.momo.furawalk.core.domain.provider.LocationData
import com.momo.furawalk.data.local.room.dao.BokkaDao
import com.momo.furawalk.data.local.room.dao.CheckpointDao
import com.momo.furawalk.data.local.room.dao.PlayerDao
import com.momo.furawalk.data.local.room.entity.*
import kotlinx.coroutines.flow.first
import java.util.UUID

class BokkaManager(
    private val bokkaDao: BokkaDao,
    private val checkpointDao: CheckpointDao,
    private val playerDao: PlayerDao
) {
    companion object {
        private const val TAG = "BokkaManager"
        private const val SPAWN_RADIUS_METERS = 1000.0
        private const val INTERACTION_RADIUS_METERS = 50.0
        private const val EVENT_DURATION_MILLIS = 24 * 60 * 60 * 1000L
    }

    /**
     * 期限切れのイベントをクリーンアップし、必要に応じて新しい歩荷さんを出現させる
     */
    suspend fun updateBokkaStatus(currentLocation: LocationData?) {
        val now = System.currentTimeMillis()
        bokkaDao.deactivateExpiredEvents(now)

        val activeEvent = bokkaDao.getActiveBokkaEvent(now).first()
        if (activeEvent == null) {
            // 現在出現していない場合、出現判定を行う (ここではデモ用に位置情報があれば50%で出現)
            if (currentLocation != null && Math.random() < 0.5) {
                spawnBokka(currentLocation)
            }
        } else {
            // 出現中の場合、到着判定を行う
            checkArrival(currentLocation, activeEvent)
        }
    }

    private suspend fun spawnBokka(userLoc: LocationData) {
        val allCheckpoints = checkpointDao.getAllCheckpoints().first()
        
        // 1000m以内の候補を探す
        val candidates = allCheckpoints.filter { cp ->
            val results = FloatArray(1)
            Location.distanceBetween(userLoc.latitude, userLoc.longitude, cp.latitude, cp.longitude, results)
            results[0] <= SPAWN_RADIUS_METERS
        }

        if (candidates.isEmpty()) {
            Log.d(TAG, "No candidate POIs within ${SPAWN_RADIUS_METERS}m. Skipping spawn.")
            return
        }

        val targetSpot = candidates.random()
        val eventId = "bokka_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val newEvent = BokkaEventEntity(
            eventId = eventId,
            spawnTime = now,
            expireTime = now + EVENT_DURATION_MILLIS,
            latitude = targetSpot.latitude,
            longitude = targetSpot.longitude,
            spotName = targetSpot.name,
            bokkaType = listOf("travel", "mountain", "flower", "food").random(),
            message = getBokkaMessage(),
            isActive = true
        )

        // 仮の商品リストを生成 (将来的にサーバーから取得)
        val mockItems = listOf(
            BokkaItemEntity(eventId = eventId, itemId = "food_special_snack", name = "万能おやつ", price = 1500, stock = 1),
            BokkaItemEntity(eventId = eventId, itemId = "food_mystery", name = "？？？フード", price = 300, stock = 3),
            BokkaItemEntity(eventId = eventId, itemId = "food_friendship_max", name = "なつきMAXクッキー", price = 5000, stock = 1)
        )

        bokkaDao.insertBokkaEvent(newEvent)
        bokkaDao.insertBokkaInventory(mockItems)
        Log.d(TAG, "Bokka spawned at ${targetSpot.name}")
    }

    private suspend fun checkArrival(userLoc: LocationData?, event: BokkaEventEntity) {
        if (userLoc == null || event.isArrived) return

        val results = FloatArray(1)
        Location.distanceBetween(userLoc.latitude, userLoc.longitude, event.latitude, event.longitude, results)
        
        if (results[0] <= INTERACTION_RADIUS_METERS) {
            bokkaDao.updateBokkaEvent(event.copy(isArrived = true))
            Log.d(TAG, "Arrived at Bokka!")
        }
    }

    private fun getBokkaMessage(): String {
        val messages = listOf(
            "おーい！珍しいものを仕入れてきたぞ！",
            "今日は遠くから歩いてきたんだ。",
            "ここでちょっと休憩していくか。",
            "いいペットを連れてるな！"
        )
        return messages.random()
    }

    /**
     * 歩荷さんからアイテムを購入する
     */
    suspend fun purchaseItem(eventId: String, itemId: String): Boolean {
        val item = bokkaDao.getItemByEventAndId(eventId, itemId) ?: return false
        if (item.stock <= 0) return false

        val currencies = playerDao.getAllCurrencies().first()
        val money = currencies.find { it.type == "MONEY" } ?: return false
        if (money.currentAmount < item.price) return false

        // 1. 在庫を減らす
        val stockUpdated = bokkaDao.purchaseItem(eventId, itemId)
        if (!stockUpdated) return false

        // 2. お金を減らす
        playerDao.updateCurrency(money.copy(
            currentAmount = money.currentAmount - item.price,
            totalSpent = money.totalSpent + item.price
        ))

        // 3. インベントリに追加
        val inventory = playerDao.getInventory().first()
        val existing = inventory.find { it.itemId == itemId }
        if (existing != null) {
            playerDao.updateInventoryItem(existing.copy(quantity = (existing.quantity + 1).coerceAtMost(30)))
        } else {
            playerDao.updateInventoryItem(InventoryEntity(itemId = itemId, quantity = 1))
        }

        return true
    }
}
