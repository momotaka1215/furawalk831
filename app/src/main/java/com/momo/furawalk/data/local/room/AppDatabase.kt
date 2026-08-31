package com.momo.furawalk.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.momo.furawalk.data.local.room.dao.CheckpointDao
import com.momo.furawalk.data.local.room.dao.PlayerDao
import com.momo.furawalk.data.local.room.dao.AvatarDao
import com.momo.furawalk.data.local.room.dao.ShopDao
import com.momo.furawalk.data.local.room.dao.PetDao
import com.momo.furawalk.data.local.room.dao.EventDao
import com.momo.furawalk.data.local.room.dao.GrowthDao
import com.momo.furawalk.data.local.room.dao.BokkaDao
import com.momo.furawalk.data.local.room.dao.TortoiseDao
import com.momo.furawalk.data.local.room.entity.*

@Database(
    entities = [
        CheckpointEntity::class,
        PlayerEntity::class,
        PlayerCurrencyEntity::class,
        PetEntity::class,
        InventoryEntity::class,
        AvatarEquipmentEntity::class,
        DiscoveredCheckpointEntity::class,
        EncounterHistoryEntity::class,
        QuestProgressEntity::class,
        DailyActivityEntity::class,
        AvatarEntity::class,
        ShopItemEntity::class,
        PetSpeciesEntity::class,
        DailyQuestEntity::class,
        EventEntity::class,
        PetGrowthRecordEntity::class,
        BokkaEventEntity::class,
        BokkaItemEntity::class,
        TortoiseEventStateEntity::class
    ],
    version = 25,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkpointDao(): CheckpointDao
    abstract fun playerDao(): PlayerDao
    abstract fun avatarDao(): AvatarDao
    abstract fun shopDao(): ShopDao
    abstract fun petDao(): PetDao
    abstract fun eventDao(): EventDao
    abstract fun growthDao(): GrowthDao
    abstract fun bokkaDao(): BokkaDao
    abstract fun tortoiseDao(): TortoiseDao
}
