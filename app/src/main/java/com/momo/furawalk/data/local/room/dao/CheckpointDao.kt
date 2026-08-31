package com.momo.furawalk.data.local.room.dao

import androidx.room.*
import com.momo.furawalk.data.local.room.entity.CheckpointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckpointDao {
    @Query("SELECT * FROM checkpoints")
    fun getAllCheckpoints(): Flow<List<CheckpointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(checkpoints: List<CheckpointEntity>)

    @Query("DELETE FROM checkpoints")
    suspend fun deleteAll()

    @Update
    suspend fun updateCheckpoint(checkpoint: CheckpointEntity)

    @Query("SELECT * FROM checkpoints WHERE id = :id LIMIT 1")
    suspend fun getCheckpointById(id: String): CheckpointEntity?

    @Query("SELECT * FROM checkpoints")
    suspend fun getAllCheckpointsList(): List<CheckpointEntity>

    @Query("SELECT * FROM checkpoints WHERE lastCalibratedAt > 0 ORDER BY lastCalibratedAt DESC")
    suspend fun getModifiedCheckpoints(): List<CheckpointEntity>
}
