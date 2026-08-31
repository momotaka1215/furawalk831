package com.momo.furawalk.data.local.room.dao

import androidx.room.*
import com.momo.furawalk.data.local.room.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    // プレイヤープロフィール
    @Query("SELECT * FROM player_profile WHERE id = :id")
    fun getPlayerProfile(id: String = "default_player"): Flow<PlayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlayer(player: PlayerEntity)

    // 通貨
    @Query("SELECT * FROM player_currencies")
    fun getAllCurrencies(): Flow<List<PlayerCurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCurrency(currency: PlayerCurrencyEntity)

    // ペット情報
    @Query("SELECT * FROM pet_status WHERE id = :id")
    fun getPetStatus(id: String = "current_pet"): Flow<PetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePetStatus(pet: PetEntity)

    // インベントリ
    @Query("SELECT * FROM inventory")
    fun getInventory(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateInventoryItem(item: InventoryEntity)

    // アバター装備
    @Query("SELECT * FROM avatar_equipment")
    fun getEquippedAvatar(): Flow<List<AvatarEquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun equipPart(equipment: AvatarEquipmentEntity)

    // 発見済みチェックポイント
    @Query("SELECT * FROM discovered_checkpoints")
    fun getDiscoveredCheckpoints(): Flow<List<DiscoveredCheckpointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordCheckpointVisit(checkpoint: DiscoveredCheckpointEntity)

    @Query("SELECT * FROM discovered_checkpoints WHERE checkpointId = :id")
    suspend fun getDiscoveredCheckpointById(id: String): DiscoveredCheckpointEntity?

    // すれ違い履歴 (最新10件のみ保持)
    @Query("SELECT * FROM encounter_history ORDER BY metAt DESC LIMIT 10")
    fun getEncounterHistory(): Flow<List<EncounterHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncounter(history: EncounterHistoryEntity)

    @Query("DELETE FROM encounter_history WHERE encounterId NOT IN (SELECT encounterId FROM encounter_history ORDER BY metAt DESC LIMIT 10)")
    suspend fun cleanupOldEncounters()

    @Transaction
    suspend fun addEncounterWithCleanup(history: EncounterHistoryEntity) {
        insertEncounter(history)
        cleanupOldEncounters()
    }

    // クエスト
    @Query("SELECT * FROM quest_progress")
    fun getQuestProgress(): Flow<List<QuestProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateQuest(quest: QuestProgressEntity)

    // 日別統計 (120日間)
    @Query("SELECT * FROM daily_activity ORDER BY date DESC LIMIT 120")
    fun getRecentActivity(): Flow<List<DailyActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyActivity(activity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity WHERE date < :thresholdDate")
    suspend fun cleanupOldActivity(thresholdDate: String)

    // デイリークエスト
    @Query("SELECT * FROM daily_quests WHERE date = :date")
    fun getDailyQuest(date: String): Flow<DailyQuestEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyQuest(dailyQuest: DailyQuestEntity)

    // カテゴリ別の訪問統計
    @Query("""
        SELECT cp.type, SUM(dc.visitCount) as totalCount 
        FROM discovered_checkpoints dc
        JOIN checkpoints cp ON dc.checkpointId = cp.id
        GROUP BY cp.type
    """)
    fun getVisitStatsByType(): kotlinx.coroutines.flow.Flow<List<TypeVisitStat>>
}

data class TypeVisitStat(
    val type: String,
    val totalCount: Int
)
