package com.momo.furawalk.data.local.room.dao

import androidx.room.*
import com.momo.furawalk.data.local.room.entity.AvatarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarDao {
    @Query("SELECT * FROM avatar_master")
    fun getAllAvatars(): Flow<List<AvatarEntity>>

    @Query("SELECT * FROM avatar_master WHERE id = :id")
    suspend fun getAvatarById(id: String): AvatarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvatar(avatar: AvatarEntity)

    @Update
    suspend fun updateAvatar(avatar: AvatarEntity)
}
