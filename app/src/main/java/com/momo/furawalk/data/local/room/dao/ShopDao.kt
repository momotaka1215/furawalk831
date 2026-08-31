package com.momo.furawalk.data.local.room.dao

import androidx.room.*
import com.momo.furawalk.data.local.room.entity.InventoryEntity
import com.momo.furawalk.data.local.room.entity.PlayerCurrencyEntity
import com.momo.furawalk.data.local.room.entity.ShopItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_catalog")
    fun getAllItems(): Flow<List<ShopItemEntity>>

    @Query("SELECT * FROM shop_catalog WHERE id = :id")
    suspend fun getItemById(id: String): ShopItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: ShopItemEntity)

    @Query("SELECT * FROM player_currencies WHERE type = 'MONEY'")
    suspend fun getPlayerMoney(): PlayerCurrencyEntity?

    @Update
    suspend fun updateCurrency(currency: PlayerCurrencyEntity)

    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    suspend fun getInventoryItem(itemId: String): InventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateInventory(item: InventoryEntity)

    @Transaction
    suspend fun purchaseItem(itemId: String): Boolean {
        val item = getItemById(itemId) ?: return false
        val money = getPlayerMoney() ?: return false
        
        if (money.currentAmount < item.price) return false
        
        // インベントリ上限チェック
        val inventory = getInventoryItem(itemId)
        if (inventory != null && inventory.quantity >= 30) {
            return false // すでに上限
        }

        // 残高更新
        val newMoney = money.copy(
            currentAmount = money.currentAmount - item.price,
            totalSpent = money.totalSpent + item.price
        )
        updateCurrency(newMoney)
        
        // インベントリ更新
        if (inventory != null) {
            updateInventory(inventory.copy(quantity = (inventory.quantity + 1).coerceAtMost(30)))
        } else {
            updateInventory(InventoryEntity(itemId = itemId, quantity = 1))
        }
        
        return true
    }
}
