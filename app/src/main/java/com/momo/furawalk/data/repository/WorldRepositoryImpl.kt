package com.momo.furawalk.data.repository

import com.momo.furawalk.core.domain.model.map.Checkpoint
import com.momo.furawalk.core.domain.repository.WorldRepository
import com.momo.furawalk.data.local.room.dao.CheckpointDao
import com.momo.furawalk.data.local.room.entity.CheckpointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DBを唯一のソースとするRepositoryの実装
 */
class WorldRepositoryImpl(
    private val checkpointDao: CheckpointDao
) : WorldRepository {

    override fun getAllCheckpoints(): Flow<List<Checkpoint>> {
        return checkpointDao.getAllCheckpoints().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateCheckpoints(checkpoints: List<Checkpoint>) {
        val existing = checkpointDao.getAllCheckpointsList()
        val newIds = checkpoints.map { it.id }.toSet()
        
        // 1. サーバー/アセットから消えた地点（かつ補正もされていない地点）を削除候補とする
        val idsToDelete = existing.filter { it.id !in newIds && (it.lastCalibratedAt ?: 0) <= 0 }.map { it.id }
        // (注: 本格的な削除ロジックが必要な場合はここにDeleteクエリを呼ぶ)

        // 2. 補正データの保護とマージ
        val entities = checkpoints.map { domain ->
            val old = existing.find { it.id == domain.id }
            
            if (old != null && (old.lastCalibratedAt ?: 0) > 0) {
                CheckpointEntity.fromDomain(domain).copy(
                    latitude = old.latitude,
                    longitude = old.longitude,
                    lastCalibratedAt = old.lastCalibratedAt
                )
            } else {
                CheckpointEntity.fromDomain(domain)
            }
        }
        
        checkpointDao.insertAll(entities)
    }

    override suspend fun syncWithServer() {
        // SyncManager経由で呼ばれるか、ここで直接Syncロジックを書く
    }
}
