package com.momo.furawalk.core.domain.repository

import com.momo.furawalk.core.domain.model.map.Checkpoint
import kotlinx.coroutines.flow.Flow

/**
 * ゲーム世界のマスターデータ（目的地リストなど）にアクセスするための抽象層
 */
interface WorldRepository {
    /**
     * 全てのチェックポイントを取得（将来的に範囲絞り込み等を追加）
     */
    fun getAllCheckpoints(): Flow<List<Checkpoint>>

    /**
     * ローカルデータを一括更新（同期用）
     */
    suspend fun updateCheckpoints(checkpoints: List<Checkpoint>)
    
    /**
     * サーバーからの最新データ同期
     */
    suspend fun syncWithServer()
}
