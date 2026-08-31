package com.momo.furawalk.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.momo.furawalk.data.local.room.entity.PetGrowthRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthDao {
    @Query("SELECT * FROM pet_growth_records WHERE isShown = 0 ORDER BY createdAt DESC")
    fun getUnshownGrowthRecords(): Flow<List<PetGrowthRecordEntity>>

    @Insert
    suspend fun insertRecord(record: PetGrowthRecordEntity)

    @Update
    suspend fun updateRecord(record: PetGrowthRecordEntity)

    @Query("UPDATE pet_growth_records SET isShown = 1 WHERE isShown = 0")
    suspend fun markAllAsShown()
}
