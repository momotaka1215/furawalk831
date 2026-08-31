package com.momo.furawalk.data.local.room.dao

import androidx.room.*
import com.momo.furawalk.data.local.room.entity.TortoiseEventStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TortoiseDao {
    @Query("SELECT * FROM tortoise_event_state WHERE eventId = :id LIMIT 1")
    fun getEventState(id: String = "achilles_and_tortoise"): Flow<TortoiseEventStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: TortoiseEventStateEntity)

    @Query("DELETE FROM tortoise_event_state WHERE eventId = :id")
    suspend fun deleteEvent(id: String)
}
